## Codebase
- Java 25
- Apache Maven 3.8.4
- Spring Boot 4.0.0-M1
- Alibaba Nacos 2025.0.0.0-preview
- -Nacos 2.5.1

## Database
- MySQL 8.0.39
- Redis 5.0.14.1
- Spring Data Redis 4.0.0-M1
- Mybatis Plus 3.5.5
- Spring Data Elasticsearch 4.0.0-M1
- Elasticsearch 7.17.5

## Util
- Slf4j 2.0.17
- Assertj Core 4.0.0-M1
- Json Util Assertj 5.1.0
- Jwt 0.12.5
- OpenApi

## Link tracing
- Zipkin Reporter Brave 2.16.3
- Micrometer tracing 1.6.0-M1


- log均使用aop切入，均需要精确到方法
- 方法中多于三个参数需要封装
- 每个方法块都需要添加javadoc，描述方法的作用，参数，返回值，异常抛出内容
  - Controller类返回同一类型（暂定JSON）