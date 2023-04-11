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
package com.tomoncle.rpc.core.client;


import com.itranswarp.compiler.JavaStringCompiler;
import com.tomoncle.rpc.core.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 动态生成 xxxStub.class 文件
 *
 * @author tomoncle
 */
@SuppressWarnings("unchecked")
public class DynamicStubFactory implements StubFactory {

    private static final Logger logger = LoggerFactory.getLogger(DynamicStubFactory.class);

    /**
     * 静态变量 STUB_SOURCE_TEMPLATE 是桩的源代码的模板，我们需要做的就是，填充模板中变量，生成桩的源码，
     * 然后动态的编译、加载这个桩就可以了。
     * <p>
     * 先来看这个模板，它唯一的这个方法中，就只有一行代码，把接口的类名、方法名和序列化后的参数封装成一个 RpcRequest 对象，
     * 调用父类 AbstractStub 中的 invokeRemote 方法，发送给服务端。
     * <p>
     * invokeRemote 方法的返回值就是序列化的调用结果，
     * 我们在模板中把这个结果反序列化之后，直接作为返回值返回给调用方就可以了。
     */
    private final static String STUB_SOURCE_TEMPLATE =
            "package %s;\n" +
                    "import %s;\n" +
                    "\n" +
                    "public class %s extends AbstractStub implements %s {\n" +
                    "    @Override\n" +
                    "    public String %s(String arg) {\n" +
                    "        return SerializeSupport.parse(\n" +
                    "                invokeRemote(\n" +
                    "                        new RpcRequest(\n" +
                    "                                \"%s\",\n" +
                    "                                \"%s\",\n" +
                    "                                SerializeSupport.serialize(arg)\n" +
                    "                        )\n" +
                    "                )\n" +
                    "        );\n" +
                    "    }\n" +
                    "}";


    @Override
    public <T> T createStub(Transport transport, Class<T> serviceClass) {
        try {
            // 填充模板
            String packageName = "com.tomoncle.rpc.core.client.stubs";
            String serializeClassName = "com.tomoncle.rpc.core.serialize.SerializeSupport";
            String stubSimpleName = serviceClass.getSimpleName() + "Stub";
            String classFullName = serviceClass.getName();
            String stubFullName = packageName + "." + stubSimpleName;
            String methodName = serviceClass.getMethods()[0].getName();

            String source = String.format(STUB_SOURCE_TEMPLATE,
                    packageName,
                    serializeClassName,
                    stubSimpleName,
                    classFullName,
                    methodName,
                    classFullName,
                    methodName);

            logger.info(String.format("动态生成Stub文件: %s.java\n\n%s", stubFullName, source));
            // 编译源代码
            JavaStringCompiler compiler = new JavaStringCompiler();
            Map<String, byte[]> results = compiler.compile(stubSimpleName + ".java", source);
            // 加载编译好的类
            Class<?> clazz = compiler.loadClass(stubFullName, results);
            // 把Transport赋值给桩
            ServiceStub stubInstance = (ServiceStub) clazz.newInstance();
            stubInstance.initTransport(transport);
            // 返回这个桩
            return (T) stubInstance;

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
