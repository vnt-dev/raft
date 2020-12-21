package org.top.core.log;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.entity.SubmitRequest;
import org.top.clientapi.entity.SubmitResponse;
import org.top.core.AppendLogEntriesExec;
import org.top.core.ClientNum;
import org.top.core.RaftServerData;
import org.top.core.machine.KvStateMachineImpl;
import org.top.core.machine.OptionEnum;
import org.top.core.machine.StateMachine;
import org.top.exception.RaftException;
import org.top.rpc.NodeGroup;
import org.top.utils.DataConstants;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lubeilin
 * @date 2020/11/19
 */
@Slf4j
public class OperationFacadeImpl implements OperationFacade {
    private static StateMachine stateMachine = new KvStateMachineImpl();
    private static Map<String, Channel> map = new ConcurrentHashMap<>();
    private Channel channel;

    public OperationFacadeImpl() {
    }

    public OperationFacadeImpl(Channel channel) {
        this.channel = channel;
    }

    @Override
    public SubmitResponse submit(SubmitRequest msg) {
        try {
            if (msg.getKey() == null) {
                return new SubmitResponse(SubmitResponse.ERROR, null, msg.getId(), "key不能为空".getBytes(StandardCharsets.UTF_8));
            }
            if (RaftServerData.isUp() && ClientNum.getNum() >= NodeGroup.getNodeGroup().majority() - 1) {
                OptionEnum optionEnum = OptionEnum.getByCode(msg.getOption());
                if (optionEnum == null) {
                    return new SubmitResponse(SubmitResponse.FAIL, null, msg.getId(), "操作命令错误".getBytes(StandardCharsets.UTF_8));
                }
                switch (optionEnum) {
                    case GET:
                        return new SubmitResponse(SubmitResponse.SUCCESS, null, msg.getId(), stateMachine.get(msg.getKey()));
                    case INCR:
                    case DECR:
                        break;
                    case HAS_KEY:
                        return new SubmitResponse(SubmitResponse.SUCCESS, null,
                                msg.getId(), stateMachine.get(msg.getKey()) == null ? DataConstants.FALSE : DataConstants.TRUE);
                    case SET:
                    case SET_IF_ABSENT:
                    case SET_IF_PRESENT:
                        if (msg.getVal() == null) {
                            return new SubmitResponse(SubmitResponse.FAIL, null, msg.getId(), "值为空".getBytes(StandardCharsets.UTF_8));
                        }
                        break;
                    default:
                }
                map.put(msg.getId(), channel);
                AppendLogEntriesExec.getInstance().signal(msg);
                return null;
            } else {
                return new SubmitResponse(SubmitResponse.TURN, RaftServerData.leaderId, msg.getId(), null);
            }
        } catch (RaftException e) {
            return new SubmitResponse(SubmitResponse.FAIL, null, msg.getId(), e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            return new SubmitResponse(SubmitResponse.ERROR, null, msg.getId(), e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void callback(String index, boolean success, byte[] data) {
        Channel channel = map.remove(index);
        if (channel != null) {
            if (success) {
                channel.writeAndFlush(new SubmitResponse(SubmitResponse.SUCCESS, null, index, data));
            } else {
                channel.writeAndFlush(new SubmitResponse(SubmitResponse.FAIL, null, index, data));
            }
        }
    }

}
