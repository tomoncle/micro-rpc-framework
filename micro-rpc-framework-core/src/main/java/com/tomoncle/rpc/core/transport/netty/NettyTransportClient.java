/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tomoncle.rpc.core.transport.netty;

import com.tomoncle.rpc.core.transport.InProcessRequests;
import com.tomoncle.rpc.core.transport.RequestHandlerRegistry;
import com.tomoncle.rpc.core.transport.Transport;
import com.tomoncle.rpc.core.transport.TransportClient;
import com.tomoncle.rpc.core.transport.netty.codec.request.RequestEncoder;
import com.tomoncle.rpc.core.transport.netty.codec.response.ResponseDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * netty客户端实现
 *
 * @author tomoncle
 */
public class NettyTransportClient implements TransportClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyTransportClient.class);
    private EventLoopGroup ioEventGroup;
    private Bootstrap bootstrap;
    private final InProcessRequests inProcessRequests;
    private List<Channel> channels = new LinkedList<>();

    public NettyTransportClient() {
        logger.info("init NettyTransportClient");
        inProcessRequests = new InProcessRequests();
    }

    private Bootstrap newBootstrap(ChannelHandler channelHandler, EventLoopGroup ioEventGroup) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
                .group(ioEventGroup)
                .handler(channelHandler)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return bootstrap;
    }

    @Override
    public Transport createTransport(SocketAddress address, long connectionTimeout) throws InterruptedException, TimeoutException {
        return new NettyTransport(createChannel(address, connectionTimeout), inProcessRequests);
    }

    private synchronized Channel createChannel(SocketAddress address, long connectionTimeout) throws InterruptedException, TimeoutException {
        if (address == null) {
            throw new IllegalArgumentException("address must not be null!");
        }
        if (ioEventGroup == null) {
            ioEventGroup = newIoEventGroup();
        }
        if (bootstrap == null){
            ChannelHandler channelHandlerPipeline = newChannelHandlerPipeline();
            bootstrap = newBootstrap(channelHandlerPipeline, ioEventGroup);
        }
            ChannelFuture channelFuture;
            Channel channel;
            channelFuture = bootstrap.connect(address);
            if (!channelFuture.await(connectionTimeout)) {
                throw new TimeoutException();
            }
            channel = channelFuture.channel();
            if (channel == null || !channel.isActive()) {
                throw new IllegalStateException();
            }
            channels.add(channel);
            return channel;
    }
    private ChannelHandler newChannelHandlerPipeline() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline()
                        .addLast(new ResponseDecoder())
                        .addLast(new RequestEncoder())
                        .addLast(new ResponseInvocationHandler(inProcessRequests));
            }
        };
    }

    private EventLoopGroup newIoEventGroup() {

        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup();
        } else {
            return new NioEventLoopGroup();
        }
    }

    @Override
    public void close() {
        for (Channel channel : channels) {
            if(null != channel) {
                channel.close();
            }
        }
        if (ioEventGroup != null) {
            ioEventGroup.shutdownGracefully();
        }
        inProcessRequests.close();
    }
}
