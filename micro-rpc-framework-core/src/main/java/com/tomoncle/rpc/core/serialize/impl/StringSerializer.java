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
package com.tomoncle.rpc.core.serialize.impl;

import com.tomoncle.rpc.core.serialize.Serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author tomoncle
 */
public class StringSerializer implements Serializer<String> {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @Override
    public int size(String entry) {
        return entry.getBytes().length;
    }

    @Override
    public void serialize(String entry, byte[] bytes, int offset, int length) {
        byte[] strBytes = entry.getBytes(DEFAULT_CHARSET);
        System.arraycopy(strBytes, 0, bytes, offset, strBytes.length);
    }

    @Override
    public String parse(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, DEFAULT_CHARSET);
    }

    @Override
    public byte type() {
        return Types.STRING;
    }

    @Override
    public Class<String> getSerializeClass() {
        return String.class;
    }

}
