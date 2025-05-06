# spring-boot-starter-rabbit-rpc
Spring Boot starter and implementation of RPC over RabbitMQ (spring-boot-starter-amqp)

# Read Me First
This project is a Spring Boot starter that enables the use of RabbitMQ as a transport layer for remote procedure calls (RPC).
It acts as an abstraction over spring-boot-starter-amqp, leveraging most of its settings while allowing some of them to be overridden.
The project uses Java interfaces as RPC contracts and Kryo 5 for RabbitMQ message serialization.

The primary use case is to define contract interfaces and argument classes in separate modules, which can then be shared across different applications.
Both the client and server must have access to the same contracts and argument classes for seamless communication.

# Getting Started
1. Add the starter to your server and client projects and common library:
```xml
<dependency>
    <groupId>io.github.tex1988</groupId>
    <artifactId>spring-boot-starter-rabbit-rpc</artifactId>
    <version>1.1.0</version>
</dependency>
```
spring-boot-starter-amqp also must be present in the classpath:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```
2. An RPC service contract needs to be defined in a common library with using the `@RabbitRpcInterface` annotation (both client and server must have RPC contract interface in the classpath):
```java
@RabbitRpcInterface(exchange = "#{properties.getExchange()}",
        queue = "#{properties.getQueue()}",
        routing = "#{properties.getRouting()}")
public interface IUserService {

    User get(@Max(100) Long id);

    User create(@Validated(User.OnCreate.class) User user);

    User update(@Validated({User.OnUpdate.class}) User user);

    @FireAndForget
    void delete(@NotNull Long id);
}
```
The `exchange`, `queue`, and `routing` properties are SpEL (Spring Expression Language) expressions that are evaluated at runtime.

Alternatively, property links `${property.name}` or direct values can be provided.

If the specified `exchange` or `queue` already exists, they will be reused. Otherwise, the starter will create them automatically.

`jakarta.validation` annotations can be used for argument validation on the server side. For more details, see the [Jakarta Validation API documentation](https://javadoc.io/doc/jakarta.validation/jakarta.validation-api/latest/index.html).

The `@FireAndForget` annotation can be used to mark a method as fire-and-forget. In this case, the client will not wait for a response from the server. By default, all void methods are synchronous and wait to confirm successful execution.

3. Server side

The `@EnableRabbitRpc` annotation must be used to enable the RPC server with property `enableServer = true`. 

This library uses Kryo 5 serialization library for message serialization.

`allowedSerializationPatterns` property must be provided to specify which packages are allowed for serialization.

For proper functionality, the starter enables serialization for the following packages by default:

- java.lang.*
- java.util.*
- io.github.tex1988.boot.rpc.rabbit.model.*

`scanBasePackages` property is used to specify the packages that will be scanned for RPC contracts (interfaces marked with `@RabbitRpcInterface`).
```java
@EnableRabbitRpc(enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.example.common.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.example.common.model.*"},
        concurrency = "5-10"
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988"})
@PropertySource({
        "classpath:application-local.properties",
})
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
```

An RPC service implementation that implements the contract interface must be provided on the server side.
```java
@Service
@RabbitRpc
public class UserService implements IUserService {

    @Override
    public User get(Long id) {
        return new User("testFirstName", "testLastName",
                "user1", "user1@test.com", "+123456789", true);
    }

    @Override
    public User create(User user) {
        return user;
    }

    @Override
    public User update(User user) {
        return user;
    }

    @Override
    @SneakyThrows
    public void delete(Long id) {
        Thread.sleep(1000);
    }
}
```
The `@RabbitRpc` annotation is used to mark a class as an RPC service implementation.
A bean of a class marked with `@RabbitRpc` is required.
The method of bean creation is flexible and can include annotations like `@Component`, `@Service`, or factory methods such as `@Bean`.

4. Client side

The `@EnableRabbitRpc` annotation must be used to enable the RPC client with property `enableClient = true`:

```java
@EnableRabbitRpc(enableClient = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.example.common.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.example.common.model.*"},
        replyTimeout = 10000L
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988"})
@PropertySource({
        "classpath:application-local.properties",
})
public class ClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
```

Description of `scanBasePackages` and `allowedSerializationPatterns` properties is the same as on the server side.

`replyTimeout` property is used to specify the time in milliseconds that the client will wait for a response from the server.

To use the RPC client, you need to inject it by the contract interface:
```java
@AllArgsConstructor
@RestController
public class Controller {

    private final IUserService userService;

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.get(id);
    }
}
```
If both the client and server implementations are in the same application, you can resolve the client bean by its name. The names of all client beans are generated using the pattern `interfaceName + "Client"`. For example, for the `IUserService` contract interface, the generated bean name will be `iUserServiceClient`.

5. Additional properties.

One application can act as both a client and a server. In this case, the `enableServer` and `enableClient` properties can be set to `true` simultaneously.

For all `@EnableRabbitRpc` properties, see the [EnableRabbitRpc](src/main/java/io/github/tex1988/boot/rpc/rabbit/annotation/EnableRabbitRpc.java) class.

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.3.3/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.3.3/maven-plugin/build-image.html)
* [Spring for RabbitMQ](https://docs.spring.io/spring-boot/docs/3.3.3/reference/htmlsingle/index.html#messaging.amqp)

### Guides
The following guides illustrate how to use some features concretely:

* [Messaging with RabbitMQ](https://spring.io/guides/gs/messaging-rabbitmq/)


### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.


