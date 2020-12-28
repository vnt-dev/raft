package org.top.core.log;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.entity.OperationState;
import org.top.clientapi.entity.SubmitRequest;
import org.top.clientapi.entity.SubmitResponse;
import org.top.core.AppendLogEntriesExec;
import org.top.core.ClientNum;
import org.top.core.RaftServerData;
import org.top.core.machine.*;
import org.top.exception.RaftException;
import org.top.rpc.NodeGroup;
import org.top.rpc.codec.ProtoBufSerializer;
import org.top.rpc.codec.Serializer;
import org.top.utils.DataConstants;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lubeilin
 * @date 2020/11/19
 */
@Slf4j
public class OperationFacadeImpl implements OperationFacade {
    private StateMachine stateMachine = new KvStateMachineImpl();
    private LogIndexSemaphore logIndexSemaphore = LogIndexSemaphore.getInstance();
    private Serializer<DefaultValue> valueSerializer = new ProtoBufSerializer<>();
    private Serializer<CompareValue> compareValueSerializer = new ProtoBufSerializer<>();
    private static ReentrantLock lock = new ReentrantLock();
    private Channel channel;


    public OperationFacadeImpl() {

    }

    private boolean isExpire(long time) {
        return time > 0 && time <= System.currentTimeMillis();
    }

    @Override
    public SubmitResponse submit(SubmitRequest msg) {
        lock.lock();
        try {
            if (msg.getKey() == null) {
                return new SubmitResponse(OperationState.FAIL, null, msg.getId(), "key不能为空".getBytes(StandardCharsets.UTF_8));
            }
            if (RaftServerData.isUp() && ClientNum.getNum() >= NodeGroup.getNodeGroup().majority() - 1) {
                OptionEnum optionEnum = OptionEnum.getByCode(msg.getOption());
                if (optionEnum == null) {
                    return new SubmitResponse(OperationState.FAIL, null, msg.getId(), "操作命令错误".getBytes(StandardCharsets.UTF_8));
                }
                if (optionEnum != OptionEnum.SET) {
                    byte[] old = stateMachine.get(msg.getKey());
                    DefaultValue oldValue = old == null ? null : valueSerializer.deserialize(old, new DefaultValue());
                    switch (optionEnum) {
                        case GET:
                            if (oldValue != null) {
                                if (isExpire(oldValue.getExpireTime())) {
                                    //过期了，要走删除key的流程
                                    msg.setOption(OptionEnum.COMPARE_AND_DEL.getCode());
                                    optionEnum = OptionEnum.COMPARE_AND_DEL;
                                    msg.setVal(old);
                                } else {
                                    return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), oldValue.getValue());
                                }
                            } else {
                                return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), null);
                            }
                            break;
                        case DEL:
                            if (oldValue == null) {
                                return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), null);
                            }
                            break;
                        case EXPIRE:
                            if (oldValue == null) {
                                return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), DataConstants.FALSE);
                            } else if (isExpire(oldValue.getExpireTime())) {
                                //过期了，要走删除key的流程
                                msg.setOption(OptionEnum.COMPARE_AND_DEL.getCode());
                                optionEnum = OptionEnum.COMPARE_AND_DEL;
                                msg.setVal(old);
                            }
                            break;
                        case INCR:
                        case DECR:
                            if (oldValue != null && isExpire(oldValue.getExpireTime())) {
                                //key过期，执行状态机如果还是旧值那就从零开始操作
                                optionEnum = optionEnum == OptionEnum.INCR ? OptionEnum.RESET_INCR : OptionEnum.RESET_DECR;
                                msg.setOption(optionEnum.getCode());
                                DefaultValue thisValue = new DefaultValue(msg.getVal(), msg.getExpireTime() == null || msg.getExpireTime() < 0 ? -1 : (System.currentTimeMillis() + msg.getExpireTime()));
                                CompareValue compareValue = new CompareValue(old, valueSerializer.serialize(thisValue));
                                msg.setVal(compareValueSerializer.serialize(compareValue));
                            }
                            break;
                        case HAS_KEY:
                            if (oldValue != null) {
                                if (isExpire(oldValue.getExpireTime())) {
                                    //过期了，要走删除key的流程
                                    msg.setOption(OptionEnum.COMPARE_AND_DEL.getCode());
                                    optionEnum = OptionEnum.COMPARE_AND_DEL;
                                    msg.setVal(old);
                                } else {
                                    return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), DataConstants.TRUE);
                                }
                            } else {
                                return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), DataConstants.FALSE);
                            }
                            break;
                        case SET_IF_ABSENT:
                            if (oldValue != null && !isExpire(oldValue.getExpireTime())) {
                                //值存在
                                return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), DataConstants.FALSE);
                            } else {
                                if (msg.getVal() == null) {
                                    return new SubmitResponse(OperationState.FAIL, null, msg.getId(), "value不能为空".getBytes(StandardCharsets.UTF_8));
                                }
                                DefaultValue thisValue = new DefaultValue(msg.getVal(), msg.getExpireTime() == null || msg.getExpireTime() < 0 ? -1 : (System.currentTimeMillis() + msg.getExpireTime()));
                                CompareValue compareValue = new CompareValue(old, valueSerializer.serialize(thisValue));
                                msg.setVal(compareValueSerializer.serialize(compareValue));
                            }
                            break;
                        case SET_IF_PRESENT:
                            if (oldValue == null) {
                                //值不存在
                                return new SubmitResponse(OperationState.SUCCESS, null, msg.getId(), DataConstants.FALSE);
                            }
                            if (isExpire(oldValue.getExpireTime())) {
                                //过期了，要走删除key的流程
                                msg.setOption(OptionEnum.COMPARE_AND_DEL.getCode());
                                optionEnum = OptionEnum.COMPARE_AND_DEL;
                                msg.setVal(old);
                            }
                            break;
                        default:
                    }
                }
                if (optionEnum == OptionEnum.SET || optionEnum == OptionEnum.SET_IF_PRESENT) {
                    if (msg.getVal() == null) {
                        return new SubmitResponse(OperationState.FAIL, null, msg.getId(), "value不能为空".getBytes(StandardCharsets.UTF_8));
                    }
                    DefaultValue thisValue = new DefaultValue(msg.getVal(), msg.getExpireTime() == null || msg.getExpireTime() < 0 ? -1 : (System.currentTimeMillis() + msg.getExpireTime()));
                    msg.setVal(valueSerializer.serialize(thisValue));
                } else if (optionEnum == OptionEnum.INCR || optionEnum == OptionEnum.DECR || optionEnum == OptionEnum.EXPIRE) {
                    DefaultValue thisValue = new DefaultValue(msg.getVal(), msg.getExpireTime() == null || msg.getExpireTime() < 0 ? -1 : (System.currentTimeMillis() + msg.getExpireTime()));
                    msg.setVal(valueSerializer.serialize(thisValue));
                }
                logIndexSemaphore.addListener(msg.getId(), channel);
                AppendLogEntriesExec.getInstance().signal(msg);
                return null;
            } else {
                return new SubmitResponse(OperationState.LEADER_TURN, RaftServerData.leaderId, msg.getId(), null);
            }
        } catch (RaftException e) {
            return new SubmitResponse(OperationState.FAIL, null, msg.getId(), e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            return new SubmitResponse(OperationState.ERROR, null, msg.getId(), e.getMessage().getBytes(StandardCharsets.UTF_8));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void open(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void close() {
        if (channel != null) {
            logIndexSemaphore.remove(channel);
        }
    }
}
