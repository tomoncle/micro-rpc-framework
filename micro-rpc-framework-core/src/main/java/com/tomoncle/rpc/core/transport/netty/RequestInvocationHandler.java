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

import com.tomoncle.rpc.core.transport.RequestHandler;
import com.tomoncle.rpc.core.transport.RequestHandlerRegistry;
import com.tomoncle.rpc.core.transport.command.Command;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty 接收所有请求数据的处理类
 * @author tomoncle
 */
@ChannelHandler.Sharable
public class RequestInvocationHandler extends SimpleChannelInboundHandler<Command> {
    private static final Logger logger = LoggerFactory.getLogger(RequestInvocationHandler.class);
    private final RequestHandlerRegistry requestHandlerRegistry;

    public RequestInvocationHandler(RequestHandlerRegistry requestHandlerRegistry) {
        this.requestHandlerRegistry = requestHandlerRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Command request) throws Exception {
        logger.info("服务端收到客户端请求："+ request);
        // 根据请求命令的 Header 中的请求类型 type，去 requestHandlerRegistry 中查找对应的请求处理器 RequestHandler
        RequestHandler handler = requestHandlerRegistry.get(request.getHeader().getType());
        if(null != handler) {
            // 然后调用请求处理器去处理请求，最后把结果发送给客户端。
            Command response = handler.handle(request);
            if(null != response) {
                channelHandlerContext.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (!channelFuture.isSuccess()) {
                            logger.warn("Write response failed!", channelFuture.cause());
                            channelHandlerContext.channel().close();
                        }
                    }
                });
            } else {
                logger.warn("Response is null!");
            }
        } else {
            throw new Exception(String.format("No handler for request with type: %d!", request.getHeader().getType()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception: ", cause);
        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        if(channel.isActive()){
            ctx.close();
        }
    }
}
