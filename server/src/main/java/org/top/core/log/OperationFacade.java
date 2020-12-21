package org.top.core.log;

import org.top.clientapi.entity.SubmitRequest;
import org.top.clientapi.entity.SubmitResponse;

/**
 * @author lubeilin
 * @date 2020/11/19
 */
public interface OperationFacade {
    /**
     * 客户端提交数据
     *
     * @param msg
     * @return
     */
    SubmitResponse submit(SubmitRequest msg);

    /**
     * 执行成功的回调
     *
     * @param index
     * @param success
     * @param data
     */
    void callback(String index, boolean success, byte[] data);
}
