package com.tomoncle.rpc.core.transport.netty;

import com.tomoncle.rpc.core.transport.InProcessRequests;
import com.tomoncle.rpc.core.transport.ResponseFuture;
import com.tomoncle.rpc.core.transport.Transport;
import com.tomoncle.rpc.core.transport.command.Command;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.concurrent.CompletableFuture;

/**
 * netty 实现 rpc 通信
 * @author tomoncle
 */
public class NettyTransport implements Transport {
    private final Channel channel;
    private final InProcessRequests inProcessRequests;

    NettyTransport(Channel channel, InProcessRequests inProcessRequests) {
        this.channel = channel;
        this.inProcessRequests = inProcessRequests;
    }

    @Override
    public  CompletableFuture<Command> send(Command request) {
        // 构建返回值
        CompletableFuture<Command> completableFuture = new CompletableFuture<>();
        try {
            // 将处理中的请求放入inProcessRequests
            inProcessRequests.put(new ResponseFuture(request.getHeader().getRequestId(), completableFuture));
            // 发送命令
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    // 处理发送失败的情况
                    if (!channelFuture.isSuccess()) {
                        completableFuture.completeExceptionally(channelFuture.cause());
                        channel.close();
                    }
                }
            });
        } catch (Throwable t) {
            // 处理发送异常
            inProcessRequests.remove(request.getHeader().getRequestId());
            completableFuture.completeExceptionally(t);
        }
        return completableFuture;
    }

}
