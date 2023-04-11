package com.tomoncle.rpc.core.serialize;


import com.tomoncle.rpc.api.spi.ServiceLoadSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用序列化拓展
 *
 * @author tomoncle
 */
@SuppressWarnings("all")
public class SerializeSupport {
    private static final Logger logger = LoggerFactory.getLogger(SerializeSupport.class);
    private static Map<Class<?>/*序列化对象类型*/, Serializer<?>/*序列化实现*/> serializerMap = new HashMap<>();
    private static Map<Byte/*序列化实现类型*/, Class<?>/*序列化对象类型*/> typeMap = new HashMap<>();

    static {
        //通过spi类加载机制，加载需要序列化的对象到内存中
        for (Serializer serializer : ServiceLoadSupport.loadAll(Serializer.class)) {
            registerType(serializer.type(), serializer.getSerializeClass(), serializer);
            logger.info("Found serializer, class: {}, type: {}.",
                    serializer.getSerializeClass().getCanonicalName(),
                    serializer.type());
        }
    }

    /**
     * 获取对象类型
     *
     * @param buffer 序列化的数据
     * @return 序列化类型
     */
    private static byte parseEntryType(byte[] buffer) {
        return buffer[0];
    }

    /**
     * 注册序列化对象及序列化的类型到内存
     *
     * @param type       序列化实现类型
     * @param eClass     序列化对象类型的Class对象
     * @param serializer 序列化实现
     * @param <E>        泛型
     */
    private static <E> void registerType(byte type, Class<E> eClass, Serializer<E> serializer) {
        serializerMap.put(eClass, serializer);
        typeMap.put(type, eClass);
    }

    /**
     * 反序列化对象
     *
     * @param buffer 存放序列化数据的字节数组
     * @param offset 数组的偏移量，从这个位置开始写入序列化数据
     * @param length 对象序列化后的长度
     * @param eClass 反序列化之后生成的对象实例
     * @param <E>    反序列化之后生成的对象类型
     * @return 反序列化之后生成的对象
     */

    private static <E> E parse(byte[] buffer, int offset, int length, Class<E> eClass) {
        Object entry = serializerMap.get(eClass).parse(buffer, offset, length);
        if (eClass.isAssignableFrom(entry.getClass())) {
            return (E) entry;
        } else {
            throw new SerializeException("Type mismatch!");
        }
    }

    /**
     * 反序列化对象
     *
     * @param buffer 存放序列化数据的字节数组
     * @param <E>    反序列化之后生成的对象类型
     * @return 反序列化之后生成的对象
     */
    public static <E> E parse(byte[] buffer) {
        return parse(buffer, 0, buffer.length);
    }

    /**
     * 反序列化对象
     *
     * @param buffer 存放序列化数据的字节数组
     * @param offset 数组的偏移量，从这个位置开始写入序列化数据
     * @param length 对象序列化后的长度
     * @param <E>    反序列化之后生成的对象类型
     * @return 反序列化之后生成的对象
     */
    private static <E> E parse(byte[] buffer, int offset, int length) {
        byte type = parseEntryType(buffer);
        @SuppressWarnings("unchecked")
        Class<E> eClass = (Class<E>) typeMap.get(type);
        if (null == eClass) {
            throw new SerializeException(String.format("Unknown entry type: %d!", type));
        } else {
            return parse(buffer, offset + 1, length - 1, eClass);
        }

    }

    /**
     * 序列化对象
     * @param entry 待序列化的对象
     * @param <E> 待序列化的对象类型
     * @return 对象序列化的字节数组
     */
    public static <E> byte[] serialize(E entry) {
        @SuppressWarnings("unchecked")
        Serializer<E> serializer = (Serializer<E>) serializerMap.get(entry.getClass());
        if (serializer == null) {
            throw new SerializeException(String.format("Unknown entry class type: %s", entry.getClass().toString()));
        }
        byte[] bytes = new byte[serializer.size(entry) + 1];
        bytes[0] = serializer.type();
        serializer.serialize(entry, bytes, 1, bytes.length - 1);
        return bytes;
    }
}
