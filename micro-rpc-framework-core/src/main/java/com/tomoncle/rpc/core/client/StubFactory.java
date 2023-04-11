package com.tomoncle.rpc.core.client;

import com.tomoncle.rpc.core.transport.Transport;

/**
 * stub 工厂
 * @author tomoncle
 */
public interface StubFactory{

    /**
     * 创建一个stub的实例
     * @param transport  Transport 对象 {@link Transport}
     * @param serviceClass Class 对象
     * @param <T> 泛型
     * @return stub 对象
     */
    <T> T createStub(Transport transport, Class<T> serviceClass);
}
