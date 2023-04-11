package com.tomoncle.rpc.core.client;

import com.tomoncle.rpc.core.transport.Transport;

/**
 * 定义一个通用的 服务Stub 接口，各个服务的stub类要实现该接口
 * @author tomoncle
 */
public interface ServiceStub {
    /**
     * 初始化通信对象
     * @param transport  Transport 对象 {@link Transport}
     */
    void initTransport(Transport transport);
}
