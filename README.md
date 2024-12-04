# spring-boot-starter-rabbit-rpc
Spring Boot starter and implementation of RPC over RabbitMQ (spring-boot-starter-amqp)

# Read Me First
This project is a Spring Boot starter that enables the use of RabbitMQ as a transport layer for remote procedure calls (RPC).
It acts as an abstraction over spring-boot-starter-amqp, leveraging most of its settings while allowing some of them to be overridden.
The project uses Java interfaces as RPC contracts and Java serialization for RabbitMQ message serialization.

The primary use case is to define contract interfaces and argument classes in separate modules, which can then be shared across different applications.
Both the client and server must have access to the same contracts and argument classes for seamless communication.

# Getting Started

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


