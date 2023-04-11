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
package com.tomoncle.rpc.core.nameservice;


import com.tomoncle.rpc.api.MicroNameService;
import com.tomoncle.rpc.core.serialize.SerializeSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 *
 * 本地注册中心
 *
 * @author tomoncle
 */
public class LocalFileMicroNameService implements MicroNameService {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileMicroNameService.class);
    private static final Collection<String> schemes = Collections.singleton("file");
    private File file;

    @Override
    public Collection<String> supportedSchemes() {
        return schemes;
    }

    @Override
    public void connect(URI nameServiceUri) {
        if(schemes.contains(nameServiceUri.getScheme())) {
            file = new File(nameServiceUri);
        } else {
            throw new RuntimeException("Unsupported scheme!");
        }
    }

    @Override
    public synchronized void registerService(String serviceName, URI uri) throws IOException {
        logger.info("Register service: {}, uri: {}.", serviceName, uri);
        // 文件可能被多个进程读写，所以这里不能使用java那些锁，
        // 原因是这些锁只能在进程内起作用，它锁不住其他进程。
        // 我们这里面必须使用由操作系统提供的文件锁
        try(RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel()) {
            FileLock lock = fileChannel.lock();
            try {
                int fileLength = (int) raf.length();
                Metadata metadata;
                byte[] bytes;
                if(fileLength > 0) {
                    bytes = new byte[(int) raf.length()];
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    while (buffer.hasRemaining()) {
                        fileChannel.read(buffer);
                    }
                    metadata = SerializeSupport.parse(bytes);
                } else {
                    metadata = new Metadata();
                }
                List<URI> uris = metadata.computeIfAbsent(serviceName, new Function<String, List<URI>>() {
                    @Override
                    public List<URI> apply(String k) {
                        return new ArrayList<>();
                    }
                });
                if(!uris.contains(uri)) {
                    uris.add(uri);
                }
                logger.info(metadata.toString());

                bytes = SerializeSupport.serialize(metadata);
                fileChannel.truncate(bytes.length);
                fileChannel.position(0L);
                fileChannel.write(ByteBuffer.wrap(bytes));
                fileChannel.force(true);
            } finally {
                lock.release();
            }
        }
    }

    @Override
    public URI lookupService(String serviceName) throws IOException {
        Metadata metadata;
        try(RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel()) {
            FileLock lock = fileChannel.lock();
            try {
                byte [] bytes = new byte[(int) raf.length()];
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    fileChannel.read(buffer);
                }
                metadata = bytes.length == 0? new Metadata(): SerializeSupport.parse(bytes);
                logger.info(metadata.toString());
            } finally {
                lock.release();
            }
        }

        List<URI> uris = metadata.get(serviceName);
        if(null == uris || uris.isEmpty()) {
            return null;
        } else {
            return uris.get(ThreadLocalRandom.current().nextInt(uris.size()));
        }
    }
}
