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
package com.tomoncle.rpc.core.transport.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 命令头
 * @author tomoncle
 */
@Data
public class Header {
    /**
     * 用于唯一标识一个请求命令
     */
    private int requestId;
    /**
     * 标识这条命令的版本号
     */
    private int version;
    /**
     * 标识这条命令的类型
     */
    private int type;

    public static class Builder{
        private Header header= new Header();

        public Builder setVersion(int version) {
            header.setVersion(version);
            return this;
        }

        public Builder setType(int type) {
            header.setType(type);
            return this;
        }

        public Builder setRequestId(int requestId) {
            header.setRequestId(requestId);
            return this;
        }

        public Header build(){
            return header;
        }
    }


    public int length() {
        return Integer.BYTES
                + Integer.BYTES
                + Integer.BYTES;
    }

}
