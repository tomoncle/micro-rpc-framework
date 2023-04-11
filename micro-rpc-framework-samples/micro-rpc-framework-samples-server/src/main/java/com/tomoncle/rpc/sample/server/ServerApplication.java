package com.tomoncle.rpc.sample.server;

import com.tomoncle.rpc.api.MicroRpcService;
import com.tomoncle.rpc.api.MicroNameService;
import com.tomoncle.rpc.api.spi.ServiceLoadSupport;
import com.tomoncle.rpc.sample.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.net.URI;

/**
 * @author tomoncle
 */
public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    /**
     * 创建一个HelloService的实现
     */
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String sayHello(String user) {
            return "hello world @" + user;
        }
    }

    public static void main(String[] args) throws Exception {

        String serviceName = HelloService.class.getCanonicalName();
        File tmpDirFile = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tmpDirFile, "simple_rpc_name_service.data");
        HelloService helloService = new HelloServiceImpl();
        logger.info("创建并启动RpcAccessPoint...");
        try (MicroRpcService microRpcService = ServiceLoadSupport.load(MicroRpcService.class);
             Closeable ignored = microRpcService.startServer()) {
            MicroNameService microNameService = microRpcService.getNameService(file.toURI());
            assert microNameService != null;
            logger.info("向RpcAccessPoint注册{}服务...", serviceName);
            URI uri = microRpcService.addServiceProvider(helloService, HelloService.class);
            logger.info("服务名: {}, 向NameService注册...", serviceName);
            microNameService.registerService(serviceName, uri);
            logger.info("开始提供服务，按任何键退出.");
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
            logger.info("Bye!");
        }
    }
}


