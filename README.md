# micro-rpc-framework 调用流程

使用Java实现一个RPC框架.

## 客户端

* 1.创建RPC服务

  ```java
  // 通过spi类加载机制加载
  MicroRpcService microRpcService = ServiceLoadSupport.load(MicroRpcService.class)
  ```

  2.创建注册中心对象

  ```java
  //在 RPC 框架中，这个 NamingService 一般称为注册中心。
  //服务端的业务代码在向 RPC 框架中注册服务之后，RPC 框架就会把这个服务的名称和地址发布到注册中心上。
  MicroNameService microNameService = microRpcService.getNameService(file.toURI());
  ```

  3.向注册中心请求服务端的地址

  ```java
  //客户端的桩在调用服务端之前，会向注册中心请求服务端的地址，请求的参数就是服务名称，
  //也就是我们上面例子中的方法签名 HelloService#hello，注册中心会返回提供这个服务的地址，
  //然后客户端再去请求服务端。
  String serviceName = HelloService.class.getCanonicalName();
  URI uri = microNameService.lookupService(serviceName);
  ```

  4.在客户端，业务代码得到的 HelloService 这个接口的实例

  

  ```java
  // 在客户端，业务代码得到的 HelloService 这个接口的实例，
  // 并不是我们在服务端提供的真正的实现类 HelloServiceImpl 的一个实例。
  // 它实际上是由 RPC 框架提供的一个代理类的实例。这个代理类有一个专属的名称，叫"桩（Stub）"
  // 
  //调用流程：
  //
  // microRpcService.getRemoteService(uri, HelloService.class) 
  //   // 调用实现类
  //   -> NettyMicroRpcService.getRemoteService(uri, HelloService.class) 
  //      // 会根据传入的远程服务地址, 创建一个Transport对象
  //      -> NettyMicroRpcService.createTransport(URI uri)
  //      // 通过 反射和JavaCompiler 在内存中生成 HelloServiceStub.class, 并返回该stub实例
  //      -> stubFactory.createStub(transport, serviceClass) 
  //  
  HelloService helloService = microRpcService.getRemoteService(uri, HelloService.class);
  ```

  5.执行`sayHello(name)`

  ```java
  // 实际调用的是动态生成的 HelloServiceStub.sayHello 方法
  //    public class HelloServiceStub extends AbstractStub implements com.tomoncle.rpc.sample.service.HelloService {
  //        @Override
  //        public String sayHello(String arg) {
  //            return SerializeSupport.parse(
  //                    invokeRemote(
  //                            new RpcRequest(
  //                                    "com.tomoncle.rpc.sample.service.HelloService",
  //                                    "sayHello",
  //                                    SerializeSupport.serialize(arg)
  //                            )
  //                    )
  //            );
  //        }
  //    }
  // 实际是执行：AbstractStub.invokeRemote()
  //             // 执行调用组装命令发起远程调用
  //             -> transport.send(requestCommand) 
  //             // 实际调用 NettyTransport.send(requestCommand) 发送数据到服务端
  //             -> NettyTransport.send(requestCommand)
  //             // 等待服务端响应。。。
  //             -> 服务端处理完通过RequestInvocationHandler会把这个响应命令发送给客户端。
  //             -> 客户端HelloServiceStub接收transport.send(requestCommand)的返回值
  //             -> 调用SerializeSupport.parse反序列化字节数组为对象，返回
  //              
  String response = helloService.sayHello(name);
  ```

  ## 服务端

  1.启动后端服务

  ```java
  // 创建RPC服务
  MicroRpcService microRpcService = ServiceLoadSupport.load(MicroRpcService.class);
  // 启动后端服务
  // microRpcService.startServer
  //   // 实际调用
  //   -> NettyMicroRpcService.startServer
  //   // 创建server并启动, 通过spi加载TransportServer实例即 NettyTransportServer
  //   -> server = ServiceLoadSupport.load(TransportServer.class);
  //   -> server.start(RequestHandlerRegistry.getInstance(), port);
  //   // NettyTransportServer 初始化过程中，加载了 RequestInvocationHandler
  //   // 这个RequestInvocationHandler就是服务端处理请求的实现类
  Closeable ignored = microRpcService.startServer()
  ```

  2.创建注册中心对象

  ```java
  //在 RPC 框架中，这个 NamingService 一般称为注册中心。
  //服务端的业务代码在向 RPC 框架中注册服务之后，RPC 框架就会把这个服务的名称和地址发布到注册中心上。
  MicroNameService microNameService = microRpcService.getNameService(file.toURI());
  ```

  3.服务端注册服务的实现实例

  ```java
  // 服务端启动后，调用该接口注册 RPC 服务 到 ServiceProviderRegistry
  // microRpcService.addServiceProvider(helloService, HelloService.class)
  // -> 调用NettyMicroRpcService.addServiceProvider(helloService, HelloService.class)
  // -> 调用ServiceProviderRegistry.addServiceProvider 
  // -> 注册 RPC 服务 到 ServiceProviderRegistry 即 RpcRequestHandler
  // -> 最后返回服务地址
  URI uri = microRpcService.addServiceProvider(helloService, HelloService.class);
  ```

  4.向注册中心注册服务

  ```java
  // 根据服务名称、服务地址注册服务到注册中心
  microNameService.registerService(serviceName, uri);
  ```

  5.后面就是服务端接收请求处理的流程

  ```java
  //RequestInvocationHandler接收请求，调用channelRead0这个方法
  //->channelRead0根据请求命令的 Header 中的请求类型 type，去 requestHandlerRegistry 中查找对应的请求处理器 RequestHandler
  //->RequestHandler然后调用.handle(request)去处理请求，最后把结果发送给客户端。
  //->实际调用的是RpcRequestHandler.handle(request)开始处理请求，分为以下步骤
  //* 1.把 request 的 payload 属性反序列化成为 RpcRequest；
  //* 2.根据 rpcRequest 中的服务名，去成员变量 serviceProviders 中查找已注册服务实现类的实例；
  //* 3.找到服务提供者之后，利用 Java 反射机制调用服务的对应方法；
  //* 4.把结果封装成响应命令并返回，在 RequestInvocationHandler中，它会把这个响应命令发送给客户端。
  ```

## 备注

* 课程参考：https://time.geekbang.org/column/intro/100032301?tab=catalog
* 课件源码：https://github.com/liyue2008/simple-rpc-framework



