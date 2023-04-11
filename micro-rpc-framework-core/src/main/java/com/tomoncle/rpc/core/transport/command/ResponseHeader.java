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
package com.tomoncle.rpc.core.transport.command;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

/**
 * 响应头
 *
 * @author tomoncle
 */
@Getter
@Setter
public class ResponseHeader extends Header {
    private int code = Code.SUCCESS.getCode();
    private String error;

    public static class Builder{
        private ResponseHeader responseHeader= new ResponseHeader();

        public Builder setVersion(int version) {
            this.responseHeader.setVersion(version);
            return this;
        }

        public Builder setType(int type) {
            this.responseHeader.setType(type);
            return this;
        }

        public Builder setRequestId(int requestId) {
            this.responseHeader.setRequestId(requestId);
            return this;
        }

        public Builder setCode(int code) {
            this.responseHeader.setCode(code);
            return this;
        }

        public Builder setError(String error) {
            this.responseHeader.setError(error);
            return this;
        }

        public Builder setHeader(Header header){
            this.setRequestId(header.getRequestId());
            this.setType(header.getType());
            this.setVersion(header.getVersion());
            return this;
        }

        public ResponseHeader build(){
            return responseHeader;
        }
    }


    @Override
    public int length() {
        return Integer.BYTES
                + Integer.BYTES
                + Integer.BYTES
                + Integer.BYTES
                + Integer.BYTES
                + (error == null ? 0 : error.getBytes(StandardCharsets.UTF_8).length);
    }


}
