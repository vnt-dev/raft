package org.top.core.log;

import org.top.rpc.entity.SubmitRequest;
import org.top.rpc.entity.SubmitResponse;

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
     * 等待回复
     */
    void await();

    /**
     * 获取结果
     *
     * @return 结果
     */
    SubmitResponse result();
}
