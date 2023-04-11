package com.tomoncle.rpc.core.serialize;

/**
 * @author tomoncle
 */
public class SerializeException  extends RuntimeException {
    public SerializeException(String msg) {
        super(msg);
    }
    public SerializeException(Throwable throwable){ super(throwable);}
}