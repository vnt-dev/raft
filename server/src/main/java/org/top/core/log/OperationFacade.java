package org.top.core.log;

import io.netty.channel.Channel;
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
     * 建立连接
     *
     * @param channel
     */
    void open(Channel channel);

    /**
     * 客户端关闭连接
     */
    void close();
}
