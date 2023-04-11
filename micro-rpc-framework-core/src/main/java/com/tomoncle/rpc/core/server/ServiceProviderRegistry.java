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
package com.tomoncle.rpc.core.server;

/**
 *
 * 服务提供者注册中心
 *
 * @author tomoncle
 */
public interface ServiceProviderRegistry {
    /**
     * 服务端启动后，调用该接口注册 RPC 服务 到 ServiceProviderRegistry
     *
     * @param serviceClass    服务接口
     * @param serviceProvider 服务提供方，也就是服务实现类的对象
     * @param <T>             泛型
     */
    <T> void addServiceProvider(Class<? extends T> serviceClass, T serviceProvider);
}
