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
package com.tomoncle.rpc.core.transport.netty.codec.request;

import com.tomoncle.rpc.core.transport.command.Header;
import com.tomoncle.rpc.core.transport.netty.codec.CommandDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * request 解码
 * @author tomoncle
 */
public class RequestDecoder extends CommandDecoder {

    @Override
    protected Header decodeHeader(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        return new Header.Builder()
                .setType(byteBuf.readInt())
                .setVersion(byteBuf.readInt())
                .setRequestId(byteBuf.readInt())
                .build();
    }
}
