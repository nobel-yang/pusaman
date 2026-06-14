# Guava RateLimit AOP 限流设计

## 目标

通过自定义注解 `@RateLimit` + AOP 切面，为任意方法提供基于 Guava `RateLimiter` 的按接口独立限流能力。触发限流时直接写 HTTP 429 响应，不抛异常。

## 架构

```
请求到达 → RateLimitAspect.around() → tryAcquire()
                                          ├─ 通过 → 执行目标方法，返回结果
                                          └─ 拒绝 → 写 429 "Too Many Requests"，返回 null
```

## 组件

### 新增依赖：`pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

Guava 已在 pom.xml 中（`33.4.8-jre`），无需重复添加。

### 新建：`annotation/RateLimit.java`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    double qps() default 10.0;  // 每秒最大请求数
}
```

### 新建：`aspect/RateLimitAspect.java`

- `@Aspect @Component`
- 持有 `ConcurrentHashMap<String, RateLimiter>`，key = `pjp.getSignature().toLongString()`（方法全限定名，唯一）
- `@Around("@annotation(rateLimit)")`
- 首次调用通过 `computeIfAbsent` 懒初始化 `RateLimiter.create(rateLimit.qps())`
- `limiter.tryAcquire()` 返回 false 时：通过 `RequestContextHolder` 获取 `HttpServletResponse`，写状态码 429 和响应体 `"Too Many Requests"`，返回 `null`
- 通过时调用 `pjp.proceed()` 正常执行

## 使用方式

```java
@GetMapping("/labels")
@RateLimit(qps = 5.0)
public ResponseEntity<List<ProblemLabel>> labels() { ... }
```

## 文件结构

| 文件 | 操作 |
|------|------|
| `pom.xml` | 修改，新增 `spring-boot-starter-aop` |
| `src/main/java/com/lab/ai/pusaman/annotation/RateLimit.java` | 新建 |
| `src/main/java/com/lab/ai/pusaman/aspect/RateLimitAspect.java` | 新建 |

## 错误处理

| 场景 | 处理 |
|------|------|
| 限流触发 | 写 429，响应体 `"Too Many Requests"`，方法返回 null |
| `RequestContextHolder` 获取不到 response（非 web 线程） | 记录 WARN 日志，放行（不限流） |

## 不变更范围

- 现有接口无需修改，`@RateLimit` 为可选注解
- 不引入 Redis 或其他分布式组件，仅单机内存限流
