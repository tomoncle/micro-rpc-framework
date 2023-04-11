package com.tomoncle.rpc.api;

import com.tomoncle.rpc.api.spi.ServiceLoadSupport;

import java.io.Closeable;
import java.net.URI;
import java.util.Collection;

/**
 * RPC框架对外提供的服务接口
 *
 * @author tomoncle
 */
public interface MicroRpcService extends Closeable {
    /**
     * 客户端获取远程服务的引用，返回一个serviceStub实例
     *
     * @param uri          远程服务地址
     * @param serviceClass 服务的接口类的Class
     * @param <T>          服务接口的类型
     * @return 远程服务引用
     */
    <T> T getRemoteService(URI uri, Class<T> serviceClass);

    /**
     * 服务端注册服务的实现实例, 并返回注册的服务地址
     *
     * @param service      服务实现类对象
     * @param serviceClass 服务的接口类的Class
     * @param <T>          服务接口的类型
     * @return 服务地址
     */
    <T> URI addServiceProvider(T service, Class<T> serviceClass);

    /**
     * 获取注册中心的引用
     *
     * @param nameServiceUri 注册中心URI
     * @return 注册中心引用
     */
    default MicroNameService getNameService(URI nameServiceUri) {
        // 过 SPI 机制加载所有的 NameService 的实现类
        Collection<MicroNameService> microNameServices = ServiceLoadSupport.loadAll(MicroNameService.class);
        for (MicroNameService microNameService : microNameServices) {
            // 根据给定的 URI 中的协议，去匹配支持这个协议的实现类，然后返回这个实现的引用
            if (microNameService.supportedSchemes().contains(nameServiceUri.getScheme())) {
                microNameService.connect(nameServiceUri);
                return microNameService;
            }
        }
        return null;
    }

    /**
     * 服务端启动RPC框架，监听接口，开始提供远程服务。
     *
     * @return 服务实例，用于程序停止的时候安全关闭服务。
     */
    Closeable startServer() throws Exception;
}
