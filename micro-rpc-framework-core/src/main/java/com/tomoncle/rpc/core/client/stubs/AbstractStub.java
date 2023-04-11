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
package com.tomoncle.rpc.core.client.stubs;



import com.tomoncle.rpc.core.client.RequestIdSupport;
import com.tomoncle.rpc.core.client.ServiceStub;
import com.tomoncle.rpc.core.client.ServiceTypes;
import com.tomoncle.rpc.core.serialize.SerializeSupport;
import com.tomoncle.rpc.core.transport.RequestHandlerRegistry;
import com.tomoncle.rpc.core.transport.Transport;
import com.tomoncle.rpc.core.transport.command.Code;
import com.tomoncle.rpc.core.transport.command.Command;
import com.tomoncle.rpc.core.transport.command.Header;
import com.tomoncle.rpc.core.transport.command.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;


/**
 * 定义一个抽象的Stub类，实现核心的远程调用逻辑
 *
 * client -> xService -> xServiceStub（代理生成，携带 服务名，方法名，参数，序列化的数据） -> server
 *
 * server -> 接收请求反序列化 -> 解析 xService -> 查找xService实现类 -> 使用传入的参数调用xService实现类.方法 -> client
 *
 *
 * @author tomoncle
 */
public abstract class AbstractStub implements ServiceStub {
    private static final Logger logger = LoggerFactory.getLogger(AbstractStub.class);
    private Transport transport;

    /**
     * 调用服务端，返回处理的结果
     *
     * @param request RPC请求对象 {@link RpcRequest}
     * @return 调用结果的序列化数组
     */
    public byte [] invokeRemote(RpcRequest request) {
        // 组装命令
        Header header = new Header.Builder()
                .setRequestId(RequestIdSupport.next())
                .setType(ServiceTypes.TYPE_RPC_REQUEST)
                .setVersion(1)
                .build();
        byte [] payload = SerializeSupport.serialize(request);
        Command requestCommand = new Command(header, payload);
        logger.info("构建数据：Header:{} ，Command:{}", header,requestCommand);
        try {
            // 调用服务端
            Command responseCommand = transport.send(requestCommand).get();
            ResponseHeader responseHeader = (ResponseHeader) responseCommand.getHeader();
            // 如果正常返回序列化后的数组
            if(responseHeader.getCode() == Code.SUCCESS.getCode()) {
                return responseCommand.getPayload();
            } else {
                throw new Exception(responseHeader.getError());
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initTransport(Transport transport) {
        this.transport = transport;
    }
}
