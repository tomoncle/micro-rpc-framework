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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * 处理中的请求，防止后端处理速度小于客户端请求速度导致后端消息堆积问题
 * <p>
 * 在异步请求中，客户端异步发送请求并不会等待服务端，缺少了这个天然的背压机制，
 * 如果服务端的处理速度跟不上客户端的请求速度，客户端的发送速度也不会因此慢下来，
 * 就会出现在途的请求越来越多，这些请求堆积在服务端的内存中，内存放不下就会一直请求失败。
 * 服务端处理不过来的时候，客户端还一直不停地发请求显然是没有意义的。
 * 为了避免这种情况，我们需要增加一个背压机制，在服务端处理不过来的时候限制一下客户端的请求速度。
 * <p>
 * <p>
 * 这个信号量有 10 个许可，我们每次往 InProcessRequests 中加入一个 ResponseFuture 的时候，
 * 需要先从信号量中获得一个许可，如果这时候没有许可了，就会阻塞当前这个线程，
 * 也就是发送请求的这个线程，直到有人归还了许可，才能继续发送请求。
 * 我们每结束一个在途请求，就归还一个许可，这样就可以保证在途请求的数量最多不超过 10 个请求，
 * 积压在服务端正在处理或者待处理的请求也不会超过 10 个。这样就实现了一个简单有效的背压机制。
 * <p>
 *
 * @author tomoncle
 * 文章链接：https://time.geekbang.org/column/article/144348
 */
public class InProcessRequests implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(InProcessRequests.class);
    // 超时设置
    private final static long TIMEOUT_SEC = 10L;
    // 定义10个信号量
    private final Semaphore semaphore = new Semaphore(10);
    // 存放正在进行中的请求
    private final Map<Integer, ResponseFuture> futureMap = new ConcurrentHashMap<>();
    // 定义一个具有循环任务的线程池
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    // 定时删除超时的任务
    private final ScheduledFuture scheduledFuture;

    public InProcessRequests() {
        // 创建定时任务
        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                InProcessRequests.this.removeTimeoutFutures();
            }
        }, TIMEOUT_SEC, TIMEOUT_SEC, TimeUnit.SECONDS);

        logger.info("初始化InProcessRequests，加载定时任务");
    }

    /**
     * 存入一个进行中的请求
     *
     * @param responseFuture ResponseFuture 对象 {@link ResponseFuture}
     * @throws InterruptedException 线程中断异常
     * @throws TimeoutException     超时
     */
    public void put(ResponseFuture responseFuture) throws InterruptedException, TimeoutException {
        logger.info("存入一个进行中的请求: {}", responseFuture.getRequestId());
        // 尝试获取许可，如果超过10个，就阻塞，10秒拿不到就超时
        if (semaphore.tryAcquire(TIMEOUT_SEC, TimeUnit.SECONDS)) {
            futureMap.put(responseFuture.getRequestId(), responseFuture);
        } else {
            throw new TimeoutException();
        }
    }

    /**
     * 删除超时的请求
     */
    private void removeTimeoutFutures() {
        Predicate<Map.Entry<Integer, ResponseFuture>> predicate = new Predicate<Map.Entry<Integer, ResponseFuture>>() {
            @Override
            public boolean test(Map.Entry<Integer, ResponseFuture> entry) {
                // 超时10秒，释放信号量
                if (System.nanoTime() - entry.getValue().getTimestamp() > TIMEOUT_SEC * 1000000000L) {
                    semaphore.release();
                    logger.info("定时删除超时的请求: {}", entry.getKey());
                    return true;
                } else {
                    return false;
                }
            }
        };
        futureMap.entrySet().removeIf(predicate);

    }

    /**
     * 删除正在处理的请求
     *
     * @param requestId 请求ID {@link com.tomoncle.rpc.core.client.RequestIdSupport}
     * @return ResponseFuture 对象 {@link ResponseFuture}
     */
    public ResponseFuture remove(int requestId) {
        logger.info("删除进行中的请求: {}", requestId);
        ResponseFuture future = futureMap.remove(requestId);
        if (null != future) {
            semaphore.release();
        }
        return future;
    }

    /**
     * 关闭
     */
    @Override
    public void close() {
        logger.info("关闭: InProcessRequests 及 定时任务.");
        scheduledFuture.cancel(true);
        scheduledExecutorService.shutdown();
    }
}
