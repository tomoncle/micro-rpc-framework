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
package com.tomoncle.rpc.api.spi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * spi 类加载器
 *
 * 在 SPI 中，每个接口在目录 META-INF/services/ 下都有一个配置文件，
 *      文件名: 接口的全类名，
 *      内容： 实现类的全类名。
 *
 * 只要把这个配置文件、接口和实现类都放到 CLASSPATH 中，就可以通过 SPI 的方式来进行加载了。
 * 加载的参数就是这个接口的 class 对象，返回值就是这个接口的所有实现类的实例，
 * 这样就在“不依赖实现类”的前提下，获得了一个实现类的实例
 *
 * 作用相当于 spring 的 ioc
 *
 * @author tomoncle
 */
public final class ServiceLoadSupport {
    private final static Map<String, Object> singletonServices = new HashMap<>();

    public synchronized static <S> S load(Class<S> service) {
        return StreamSupport.
                stream(ServiceLoader.load(service).spliterator(), false)
                .map(ServiceLoadSupport::singletonFilter)
                .findFirst().orElseThrow(ServiceLoadException::new);
    }

    public synchronized static <S> Collection<S> loadAll(Class<S> service) {
        return StreamSupport.
                stream(ServiceLoader.load(service).spliterator(), false)
                .map(ServiceLoadSupport::singletonFilter).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static <S> S singletonFilter(S service) {
        if (service.getClass().isAnnotationPresent(Singleton.class)) {
            String className = service.getClass().getCanonicalName();
            Object singletonInstance = singletonServices.putIfAbsent(className, service);
            return singletonInstance == null ? service : (S) singletonInstance;
        } else {
            return service;
        }
    }
}
