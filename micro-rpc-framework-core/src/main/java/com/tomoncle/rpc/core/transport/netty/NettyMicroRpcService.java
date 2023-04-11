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


import com.tomoncle.rpc.api.MicroRpcService;
import com.tomoncle.rpc.api.spi.ServiceLoadSupport;
import com.tomoncle.rpc.core.client.StubFactory;
import com.tomoncle.rpc.core.server.ServiceProviderRegistry;
import com.tomoncle.rpc.core.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * RPC框架对外提供的服务接口Netty实现
 *
 * @author tomoncle
 */
public class NettyMicroRpcService implements MicroRpcService {
    private static final Logger logger = LoggerFactory.getLogger(NettyMicroRpcService.class);
    private final String host = "localhost";
    private final int port = 9999;
    private final URI uri = URI.create("rpc://" + host + ":" + port);
    private TransportServer server = null;
    private TransportClient client = ServiceLoadSupport.load(TransportClient.class);
    private final Map<URI, Transport> clientMap = new ConcurrentHashMap<>();
    private final StubFactory stubFactory = ServiceLoadSupport.load(StubFactory.class);
    private final ServiceProviderRegistry serviceProviderRegistry = ServiceLoadSupport.load(ServiceProviderRegistry.class);

    @Override
    public <T> T getRemoteService(URI uri, Class<T> serviceClass) {
        logger.info("客户端获取远程服务的引用, URI: {}, Service:{}", uri.toASCIIString(), serviceClass.getName());
        // 如果transport不存在，就创建一个 transport， 并加入到clientMap
        Transport transport = clientMap.computeIfAbsent(uri, new Function<URI, Transport>() {
            @Override
            public Transport apply(URI uri1) {
                return NettyMicroRpcService.this.createTransport(uri1);
            }
        });
        // 返回一个 stub 实例
        return stubFactory.createStub(transport, serviceClass);
    }

    private Transport createTransport(URI uri) {
        logger.info("客户端创建Transport, URI: {}", uri.toASCIIString());
        try {
            return client.createTransport(new InetSocketAddress(uri.getHost(), uri.getPort()),30000L);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public synchronized <T> URI addServiceProvider(T service, Class<T> serviceClass) {
        logger.info("服务端注册服务的实例,Service-> {}:{} ；并返回注册的服务地址, URI: {}",
                serviceClass.getName(),
                service.getClass().getName(),
                uri.toASCIIString());
        serviceProviderRegistry.addServiceProvider(serviceClass, service);
        return uri;
    }

    @Override
    public synchronized Closeable startServer() throws Exception {
        if (null == server) {
            logger.info("启动NettyTransportServer: [::{}]",port);
            server = ServiceLoadSupport.load(TransportServer.class);
            server.start(RequestHandlerRegistry.getInstance(), port);
        }
        return new Closeable() {
            @Override
            public void close() throws IOException {
                if (null != server) {
                    server.stop();
                }
            }
        };
    }

    @Override
    public void close() {
        logger.info("关闭NettyTransportServer！");
        if(null != server) {
            server.stop();
        }
        client.close();
    }
}
