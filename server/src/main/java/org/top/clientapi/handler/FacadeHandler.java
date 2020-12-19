package org.top.clientapi.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.top.core.log.OperationFacade;
import org.top.core.log.OperationFacadeImpl;
import org.top.clientapi.entity.SubmitRequest;
import org.top.clientapi.entity.SubmitResponse;
import org.top.rpc.handler.BaseMessageHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 接收客户端请求
 *
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class FacadeHandler extends BaseMessageHandler<SubmitRequest> {
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 1000, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1000), (ThreadFactory) Thread::new);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SubmitRequest msg) {
        OperationFacade facade = new OperationFacadeImpl();
        SubmitResponse response = facade.submit(msg);
        if (response != null) {
            ctx.writeAndFlush(response);
        } else {
            try {
                executor.execute(() -> {
                    facade.await();
                    ctx.writeAndFlush(facade.result());
                });
            } catch (Exception e) {
                facade.await();
                ctx.writeAndFlush(facade.result());
            }

        }
    }

}
