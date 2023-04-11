package com.tomoncle.rpc.core.transport;

import com.tomoncle.rpc.core.transport.command.Command;

import java.util.concurrent.CompletableFuture;

/**
 * 定义通信接口
 * @author tomoncle
 */
public interface Transport {
    /**
     * 发送请求命令
     * @param request 请求命令
     * @return 返回值是一个Future，Future
     */
    CompletableFuture<Command> send(Command request);
}
