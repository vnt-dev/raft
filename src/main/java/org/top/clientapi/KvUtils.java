package org.top.clientapi;

import lombok.extern.slf4j.Slf4j;
import org.top.core.machine.OptionEnum;
import org.top.rpc.entity.SubmitResponse;
import org.top.rpc.utils.PropertiesUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.top.clientapi.ResultEntity.getEntity;

/**
 * @author lubeilin
 * @date 2020/11/27
 */
@Slf4j
public class KvUtils {
    private ApiClient apiClient = ApiClient.getApiClient();
    private static long outTime = PropertiesUtil.getLong("outTime");
    private static final int TRY_NUM = 3;

    public void delete(String key) {
        ResultEntity resultEntity = getEntity(OptionEnum.DEL.getCode(), key, null);
        send(resultEntity, 0);
    }


    public String get(String key) {
        ResultEntity resultEntity = getEntity(OptionEnum.GET.getCode(), key, null);
        send(resultEntity, 0);
        if (resultEntity.getResponse().getData() == null) {
            return null;
        }
        return new String(resultEntity.getResponse().getData());
    }


    public void set(String key, String value) {
        ResultEntity resultEntity = getEntity(OptionEnum.SET.getCode(), key, value);
        send(resultEntity, 0);
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
                    case SubmitResponse.ERROR:
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
