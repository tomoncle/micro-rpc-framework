package com.tomoncle.rpc.sample.client;

import com.tomoncle.rpc.api.MicroNameService;
import com.tomoncle.rpc.api.MicroRpcService;
import com.tomoncle.rpc.api.spi.ServiceLoadSupport;
import com.tomoncle.rpc.sample.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author tomoncle
 */
public class ClientApplication {
    private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String serviceName = HelloService.class.getCanonicalName();
        File tmpDirFile = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tmpDirFile, "simple_rpc_name_service.data");
        String name = ClientApplication.class.getSimpleName();
        AtomicInteger size = new AtomicInteger(1);
        List<Thread> threadList = new ArrayList<>();
        MicroRpcService microRpcService = ServiceLoadSupport.load(MicroRpcService.class);
        MicroNameService microNameService = microRpcService.getNameService(file.toURI());
        assert microNameService != null;
        URI uri = microNameService.lookupService(serviceName);
        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        assert uri != null;
                        logger.info("找到服务{}，提供者: {}.", serviceName, uri);
                        HelloService helloService = microRpcService.getRemoteService(uri, HelloService.class);
                        logger.info("请求服务, name: {}{}...", name, Thread.currentThread().getName());
                        String response = helloService.sayHello(name + Thread.currentThread().getName());
                        logger.info("收到响应: {}.", response);
                    }finally {
                        logger.warn("线程结束：" + size.getAndIncrement());
                    }
                }
            });
            // 设置为守护线程
            thread.setDaemon(true);
            thread.start();
            threadList.add(thread);
        }

        // 等待线程处理完
        while (size.get() <= 10) {
            logger.warn("size: "+ size.get());
            for (Thread thread : threadList) {
                logger.warn(thread.getName() + " -》 是否中断：" + thread.isInterrupted() + " , 是否活动：" + thread.isAlive());
            }
            Thread.sleep(1000);
        }
        for (Thread thread : threadList) {
            logger.warn(thread.getName() + " -》 是否中断：" + thread.isInterrupted() + " , 是否活动：" + thread.isAlive());
        }
        microRpcService.close();
        logger.warn("Main 方法结束了. size:{}", size.get());


    }
}
