package org.top.clientapi.async;

import org.top.clientapi.OptionEnum;
import org.top.rpc.ApiClient;


/**
 * 异步命令执行器
 * @author lubeilin
 * @date 2020/12/19
 */
public class AsyncCmdExecutor {
    private ApiClient apiClient = ApiClient.getApiClient();

    public void cmd(OptionEnum optionEnum, byte[] key, byte[] value, ResponseCallback callback) {
        this.cmd(optionEnum, key, value, null, callback);
    }

    /**
     * 异步执行命令，只会发送命令，执行结果由回调函数通知
     * @param optionEnum 命令类型
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间，
     * @param callback
     */
    public void cmd(OptionEnum optionEnum, byte[] key, byte[] value, Long expireTime, ResponseCallback callback) {
        AsyncResultEntity resultEntity = AsyncResultEntity.getEntity(optionEnum.getCode(), key, value, expireTime, callback);
        apiClient.send(resultEntity.getRequest());
    }
}
