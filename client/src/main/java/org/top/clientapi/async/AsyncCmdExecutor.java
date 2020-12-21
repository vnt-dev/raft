package org.top.clientapi.async;

import org.top.clientapi.OptionEnum;
import org.top.rpc.ApiClient;


/**
 * @author lubeilin
 * @date 2020/12/19
 */
public class AsyncCmdExecutor {
    private ApiClient apiClient = ApiClient.getApiClient();


    public void cmd(OptionEnum optionEnum, byte[] key, byte[] value, ResponseCallback callback) {
        AsyncResultEntity resultEntity = AsyncResultEntity.getEntity(optionEnum.getCode(), key, value, callback);
        apiClient.send(resultEntity.getRequest());
    }
}