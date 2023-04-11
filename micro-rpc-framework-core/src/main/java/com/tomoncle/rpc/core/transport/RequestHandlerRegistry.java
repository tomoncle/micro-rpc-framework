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
package com.tomoncle.rpc.core.transport;

import com.tomoncle.rpc.api.spi.ServiceLoadSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * 请求处理器注册中心
 *
 * 这种通过“请求中的类型”，把请求分发到对应的处理类或者处理方法的设计，
 * 我们在 RocketMQ 和 Kafka 的源代码中都见到过，在服务端处理请求的场景中，这是一个很常用的方法。
 *
 * 我们这里使用的也是同样的设计，不同的是，我们使用了一个命令注册机制，
 * 让这个路由分发的过程省略了大量的 if-else 或者是 switch 代码。
 * 这样做的好处是，可以很方便地扩展命令处理器，而不用修改路由分发的方法，并且代码看起来更加优雅。
 *
 * @author tomoncle
 * 文章：https://time.geekbang.org/column/article/148482
 */
public class RequestHandlerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandlerRegistry.class);
    private Map<Integer, RequestHandler> handlerMap = new HashMap<>();
    private static RequestHandlerRegistry instance = null;

    public static RequestHandlerRegistry getInstance() {
        if (null == instance) {
            instance = new RequestHandlerRegistry();
        }
        return instance;
    }

    private RequestHandlerRegistry() {
        // 通过spi类加载，注册RequestHandler
        Collection<RequestHandler> requestHandlers = ServiceLoadSupport.loadAll(RequestHandler.class);
        for (RequestHandler requestHandler : requestHandlers) {
            handlerMap.put(requestHandler.type(), requestHandler);
            logger.info("Load request handler, type: {}, class: {}.", requestHandler.type(), requestHandler.getClass().getCanonicalName());
        }
    }

    public RequestHandler get(int type) {
        return handlerMap.get(type);
    }
}
