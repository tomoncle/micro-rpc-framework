/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tomoncle.rpc.core.server;

import com.tomoncle.rpc.api.spi.Singleton;
import com.tomoncle.rpc.core.client.ServiceTypes;
import com.tomoncle.rpc.core.client.stubs.RpcRequest;
import com.tomoncle.rpc.core.serialize.SerializeSupport;
import com.tomoncle.rpc.core.transport.RequestHandler;
import com.tomoncle.rpc.core.transport.command.Code;
import com.tomoncle.rpc.core.transport.command.Command;
import com.tomoncle.rpc.core.transport.command.Header;
import com.tomoncle.rpc.core.transport.command.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务端请求处理类
 * <p>
 * 这个类不仅实现了处理客户端请求的 RequestHandler 接口，
 * 同时还实现了注册 RPC 服务 ServiceProviderRegistry 接口，
 * 也就是说，RPC 框架服务端需要实现的两个功能
 * 1.注册 RPC 服务
 * 2.处理客户端 RPC 请求
 * 都是在这一个类 RpcRequestHandler 中实现的，所以说，这个类是这个 RPC 框架服务端最核心的部分
 *
 * @author tomoncle
 */
@Singleton
public class RpcRequestHandler implements RequestHandler, ServiceProviderRegistry {
    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);
    private Map<String/*service name*/, Object/*service provider*/> serviceProviders = new HashMap<>();

    /**
     * 1.把 requestCommand 的 payload 属性反序列化成为 RpcRequest；
     * 2.根据 rpcRequest 中的服务名，去成员变量 serviceProviders 中查找已注册服务实现类的实例；
     * 3.找到服务提供者之后，利用 Java 反射机制调用服务的对应方法；
     * 4.把结果封装成响应命令并返回，在 RequestInvocationHandler 中，它会把这个响应命令发送给客户端。
     *
     * @param requestCommand 请求命令
     * @return Command
     */
    @Override
    public Command handle(Command requestCommand) {
        logger.info("处理请求：" + requestCommand);
        Header header = requestCommand.getHeader();
        // 从payload中反序列化RpcRequest
        RpcRequest rpcRequest = SerializeSupport.parse(requestCommand.getPayload());
        try {
            // 查找所有已注册的服务提供方，寻找rpcRequest中需要的服务
            Object serviceProvider = serviceProviders.get(rpcRequest.getInterfaceName());
            if (serviceProvider != null) {
                logger.info("查找服务提供者：{}", serviceProvider.getClass().getName());
                // 找到服务提供者，利用Java反射机制调用服务的对应方法
                String arg = SerializeSupport.parse(rpcRequest.getSerializedArguments());
                Method method = serviceProvider.getClass().getMethod(rpcRequest.getMethodName(), String.class);
                String result = (String) method.invoke(serviceProvider, arg);
                // 把结果封装成响应命令并返回
                logger.info("执行反射：{}.invoke({}, {})\n", method.getName(), serviceProvider.getClass().getName(), arg);
                return new Command(new ResponseHeader.Builder().setHeader(header).build(), SerializeSupport.serialize(result));
            }
            // 如果没找到，返回NO_PROVIDER错误响应。
            logger.warn("No service Provider of {}#{}(String)!", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
            return new Command(new ResponseHeader.Builder().setHeader(header).setCode(Code.NO_PROVIDER.getCode()).setError("No provider!").build(), new byte[0]);
        } catch (Throwable t) {
            // 发生异常，返回UNKNOWN_ERROR错误响应。
            logger.warn("Exception: ", t);
            return new Command(new ResponseHeader.Builder().setHeader(header).setCode(Code.UNKNOWN_ERROR.getCode()).setError(t.getMessage()).build(), new byte[0]);
        }
    }

    @Override
    public int type() {
        return ServiceTypes.TYPE_RPC_REQUEST;
    }

    /**
     * 服务端启动后，调用该接口注册 RPC 服务 到 ServiceProviderRegistry
     *
     * @param serviceClass    服务接口
     * @param serviceProvider 服务提供方，也就是服务实现类的实例
     * @param <T>             泛型
     */
    @Override
    public synchronized <T> void addServiceProvider(Class<? extends T> serviceClass, T serviceProvider) {
        // Key 就是服务名，Value 就是服务提供方，也就是服务实现类的实例
        serviceProviders.put(serviceClass.getCanonicalName(), serviceProvider);
        logger.info("Add service: {}, provider: {}.",
                serviceClass.getCanonicalName(),
                serviceProvider.getClass().getCanonicalName());
    }
}
