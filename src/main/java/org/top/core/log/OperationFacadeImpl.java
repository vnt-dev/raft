package org.top.core.log;

import lombok.extern.slf4j.Slf4j;
import org.top.core.AppendEntriesComponent;
import org.top.core.ClientNum;
import org.top.core.RaftServerData;
import org.top.core.ServerStateEnum;
import org.top.core.machine.KvStateMachineImpl;
import org.top.core.machine.OptionEnum;
import org.top.core.machine.StateMachine;
import org.top.exception.RaftException;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.rpc.NodeGroup;
import org.top.rpc.entity.SubmitRequest;
import org.top.rpc.entity.SubmitResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author lubeilin
 * @date 2020/11/19
 */
@Slf4j
public class OperationFacadeImpl implements OperationFacade {
    private static AppendEntriesComponent appendEntriesComponent = new AppendEntriesComponent();
    private static StateMachine stateMachine = new KvStateMachineImpl();
    private static LogIndexSemaphore semaphore = new LogIndexSemaphore();
    private SubmitResponse result;
    private long index;
    private byte[] id;

    @Override
    public SubmitResponse submit(SubmitRequest msg) {
        try {
            if (msg.getKey() == null) {
                result = new SubmitResponse(SubmitResponse.ERROR, null, msg.getId(), "key不能为空".getBytes(StandardCharsets.UTF_8));
                return result;
            }
            if (RaftServerData.isUp() && ClientNum.getNum() >= NodeGroup.getNodeGroup().majority() - 1) {
                LogEntry logEntry = new LogEntry();

                OptionEnum optionEnum = OptionEnum.getByCode(msg.getOption());
                if (optionEnum == null) {
                    result = new SubmitResponse(SubmitResponse.FAIL, null, msg.getId(), "操作命令错误".getBytes(StandardCharsets.UTF_8));
                    return result;
                }
                switch (optionEnum) {
                    case GET:
                        result = new SubmitResponse(SubmitResponse.SUCCESS, null, msg.getId(), stateMachine.get(msg.getKey()));
                        return result;
                    case DEL:
                    case INCR:
                    case DECR:
                    case SET:
                        logEntry.setVal(msg.getVal());
                        break;
                    default:
                        result = new SubmitResponse(SubmitResponse.FAIL, null, msg.getId(), "操作命令错误".getBytes(StandardCharsets.UTF_8));
                        return result;
                }

                PersistentStateModel model = PersistentStateModel.getModel();
                logEntry.setKey(msg.getKey());
                logEntry.setId(msg.getId());
                logEntry.setOption(msg.getOption());
                model.pushLast(logEntry);
                index = logEntry.getIndex();
                semaphore.addListener(index);
                id = msg.getId();
                appendEntriesComponent.broadcastAppendEntries();
                return null;
            } else {
                result = new SubmitResponse(SubmitResponse.TURN, RaftServerData.leaderId, msg.getId(), null);
                return result;
            }
        } catch (RaftException e) {
            result = new SubmitResponse(SubmitResponse.FAIL, null, msg.getId(), e.getMessage().getBytes(StandardCharsets.UTF_8));
            return result;
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            result = new SubmitResponse(SubmitResponse.ERROR, null, msg.getId(), e.getMessage().getBytes(StandardCharsets.UTF_8));
            return result;
        }
    }

    @Override
    public void await() {
        if (result == null) {
            try {
                if (semaphore.await(index, 20, TimeUnit.SECONDS)) {
                    LogIndexSemaphore.IndexNode indexNode = semaphore.getData(index);
                    if (indexNode.success) {
                        result = new SubmitResponse(SubmitResponse.SUCCESS, null, id, indexNode.data);
                    } else {
                        result = new SubmitResponse(SubmitResponse.FAIL, null, id, indexNode.data);
                    }
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.remove(index);
            }
            if (RaftServerData.serverStateEnum == ServerStateEnum.LEADER
                    && ClientNum.getNum() >= NodeGroup.getNodeGroup().majority() - 1) {
                result = new SubmitResponse(SubmitResponse.ERROR, null, id, "等待超时".getBytes(StandardCharsets.UTF_8));
            } else {
                result = new SubmitResponse(SubmitResponse.ERROR, null, id, "集群错误".getBytes(StandardCharsets.UTF_8));
            }
        }

    }

    @Override
    public SubmitResponse result() {
        return result;
    }
}
