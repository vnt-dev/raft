package org.top.clientapi.sync;

import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.OptionEnum;
import org.top.clientapi.entity.SubmitResponse;
import org.top.clientapi.util.PropertiesUtil;
import org.top.rpc.ApiClient;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.top.clientapi.sync.ResultEntity.getEntity;

/**
 * @author lubeilin
 * @date 2020/11/27
 */
@Slf4j
public class CmdExecutor {
    private static final int TRY_NUM = 3;
    private static long outTime = PropertiesUtil.getLong("outTime");
    private ApiClient apiClient = ApiClient.getApiClient();

    public byte[] cmd(OptionEnum optionEnum, byte[] key, byte[] value) {
        ResultEntity resultEntity = getEntity(optionEnum.getCode(), key, value);
        send(resultEntity, 0);
        return resultEntity.getResponse().getData();
    }

    private void send(ResultEntity resultEntity, int num) {
        if (num > TRY_NUM) {
            log.info("发送超时，请求：{}，响应：{}", resultEntity.getRequest(), resultEntity.getResponse());
            throw new RuntimeException("发送超时");
        }
        apiClient.send(resultEntity.getRequest());
        await(resultEntity, num);
    }

    private void await(ResultEntity resultEntity, int num) {
        try {
            if (resultEntity.getSemaphore().tryAcquire(outTime, TimeUnit.MILLISECONDS)) {
                SubmitResponse response = resultEntity.getResponse();
                switch (response.getCode()) {
                    case SubmitResponse.FAIL:
                        throw new RuntimeException(new String(response.getData(), StandardCharsets.UTF_8));
                    case SubmitResponse.ERROR:
                        apiClient.resetLeader();
                        throw new RuntimeException(new String(response.getData(), StandardCharsets.UTF_8));
                    case SubmitResponse.SUCCESS:
                        ResultEntity.remove(resultEntity);
                        return;
                    case SubmitResponse.TURN:
                        apiClient.setLeader(response.getLeaderId());
                        send(resultEntity, ++num);
                        return;
                    default:
                        throw new RuntimeException("code错误");
                }
            }
            throw new RuntimeException("响应超时");
        } catch (RuntimeException e) {
            ResultEntity.remove(resultEntity);
            throw e;
        } catch (Exception e) {
            ResultEntity.remove(resultEntity);
            throw new RuntimeException(e.getMessage());
        }
    }
}
