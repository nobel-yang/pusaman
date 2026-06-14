# Guava RateLimit AOP 限流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过 `@RateLimit(qps)` 注解 + AOP 切面，为任意 Spring Bean 方法提供基于 Guava RateLimiter 的按方法独立限流，触发时直接写 HTTP 429。

**Architecture:** 新增 `spring-boot-starter-aop` 依赖；新建 `@RateLimit` 注解（qps 参数）；新建 `RateLimitAspect`，持有 `ConcurrentHashMap<String, RateLimiter>`，`@Around` 拦截后懒初始化 RateLimiter，`tryAcquire()` 失败时写 429 返回 null。

**Tech Stack:** Spring Boot 3.5, Spring AOP (`@Aspect`), Guava `RateLimiter`（已在 pom.xml）, JUnit 5

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `pom.xml` | 修改 | 新增 `spring-boot-starter-aop` |
| `src/main/java/com/lab/ai/pusaman/annotation/RateLimit.java` | 新建 | 限流注解，`qps` 参数 |
| `src/main/java/com/lab/ai/pusaman/aspect/RateLimitAspect.java` | 新建 | AOP 切面，限流逻辑 |
| `src/test/java/com/lab/ai/pusaman/aspect/RateLimitAspectTest.java` | 新建 | 切面单元测试 |

---

### Task 1: 新增 AOP 依赖 + 定义 @RateLimit 注解

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/lab/ai/pusaman/annotation/RateLimit.java`

- [ ] **Step 1: 在 pom.xml 中新增 spring-boot-starter-aop 依赖**

在 `pom.xml` 的 `<dependencies>` 块中，在 Lombok 依赖之前新增：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

- [ ] **Step 2: 创建注解目录并新建 RateLimit.java**

```bash
mkdir -p /Users/yangkegang/workspace/ai-lab/pusaman/src/main/java/com/lab/ai/pusaman/annotation
```

新建 `src/main/java/com/lab/ai/pusaman/annotation/RateLimit.java`：

```java
package com.lab.ai.pusaman.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    double qps() default 10.0;
}
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw compile -q 2>&1 | tail -10
```

预期：无错误输出

- [ ] **Step 4: Commit**

```bash
git add pom.xml \
        src/main/java/com/lab/ai/pusaman/annotation/RateLimit.java
git commit -m "feat: add spring-boot-starter-aop and @RateLimit annotation"
```

---

### Task 2: 实现 RateLimitAspect

**Files:**
- Create: `src/main/java/com/lab/ai/pusaman/aspect/RateLimitAspect.java`
- Create: `src/test/java/com/lab/ai/pusaman/aspect/RateLimitAspectTest.java`

- [ ] **Step 1: 写失败测试**

```bash
mkdir -p /Users/yangkegang/workspace/ai-lab/pusaman/src/test/java/com/lab/ai/pusaman/aspect
```

新建 `src/test/java/com/lab/ai/pusaman/aspect/RateLimitAspectTest.java`：

```java
package com.lab.ai.pusaman.aspect;

import com.lab.ai.pusaman.annotation.RateLimit;
import com.lab.ai.pusaman.annotation.RateLimitAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitAspectTest {

    RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect();
        RequestContextHolder.resetRequestAttributes();
    }

    private ProceedingJoinPoint mockJoinPoint(String methodName) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toLongString()).thenReturn(methodName);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.proceed()).thenReturn("ok");
        return pjp;
    }

    private RateLimit mockAnnotation(double qps) {
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.qps()).thenReturn(qps);
        return annotation;
    }

    @Test
    void proceeds_when_rate_not_exceeded() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint("com.example.TestClass.method()");
        RateLimit annotation = mockAnnotation(1000.0); // 极高 QPS，不会触发限流

        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest(), response));

        Object result = aspect.around(pjp, annotation);

        assertEquals("ok", result);
        assertEquals(200, response.getStatus());
        verify(pjp, times(1)).proceed();
    }

    @Test
    void returns_429_when_rate_exceeded() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint("com.example.TestClass.slowMethod()");
        RateLimit annotation = mockAnnotation(0.0001); // 极低 QPS，立即触发限流

        // 先调用一次，消耗掉令牌
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest(), firstResponse));
        aspect.around(pjp, annotation);

        // 立即再调用，应触发限流
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest(), secondResponse));
        Object result = aspect.around(pjp, annotation);

        assertNull(result);
        assertEquals(429, secondResponse.getStatus());
        assertEquals("Too Many Requests", secondResponse.getContentAsString());
    }

    @Test
    void different_methods_have_independent_limiters() throws Throwable {
        ProceedingJoinPoint pjp1 = mockJoinPoint("com.example.TestClass.method1()");
        ProceedingJoinPoint pjp2 = mockJoinPoint("com.example.TestClass.method2()");
        RateLimit annotation1 = mockAnnotation(1000.0);
        RateLimit annotation2 = mockAnnotation(1000.0);

        MockHttpServletResponse res = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest(), res));

        Object r1 = aspect.around(pjp1, annotation1);
        Object r2 = aspect.around(pjp2, annotation2);

        assertEquals("ok", r1);
        assertEquals("ok", r2);
        verify(pjp1, times(1)).proceed();
        verify(pjp2, times(1)).proceed();
    }

    @Test
    void returns_null_gracefully_when_no_request_context() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint("com.example.TestClass.noContextMethod()");
        RateLimit annotation = mockAnnotation(0.0001);

        // 先消耗令牌
        MockHttpServletResponse res = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest(), res));
        aspect.around(pjp, annotation);

        // 无 RequestContext 时触发限流
        RequestContextHolder.resetRequestAttributes();
        Object result = aspect.around(pjp, annotation);

        assertNull(result);
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw test -Dtest=RateLimitAspectTest -q 2>&1 | tail -20
```

预期：编译错误，`cannot find symbol: class RateLimitAspect`

- [ ] **Step 3: 创建 aspect 目录并实现 RateLimitAspect**

```bash
mkdir -p /Users/yangkegang/workspace/ai-lab/pusaman/src/main/java/com/lab/ai/pusaman/aspect
```

新建 `src/main/java/com/lab/ai/pusaman/aspect/RateLimitAspect.java`：

```java
package com.lab.ai.pusaman.aspect;

import com.google.common.util.concurrent.RateLimiter;
import com.lab.ai.pusaman.annotation.RateLimit;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = pjp.getSignature().toLongString();
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> RateLimiter.create(rateLimit.qps()));

        if (limiter.tryAcquire()) {
            return pjp.proceed();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("RateLimit triggered on [{}] but no request context available", key);
            return null;
        }

        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            log.warn("RateLimit triggered on [{}] but response is null", key);
            return null;
        }

        response.setStatus(429);
        response.getWriter().write("Too Many Requests");
        return null;
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
./mvnw test -Dtest=RateLimitAspectTest -q 2>&1 | tail -10
```

预期：`Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: 运行全量测试**

```bash
./mvnw test -q 2>&1 | grep -E "Tests run:|BUILD" | tail -10
```

预期：全部通过，BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/aspect/RateLimitAspect.java \
        src/test/java/com/lab/ai/pusaman/aspect/RateLimitAspectTest.java
git commit -m "feat: add RateLimitAspect with Guava RateLimiter"
```

---

### Task 3: 在现有接口上添加 @RateLimit 示例

**Files:**
- Modify: `src/main/java/com/lab/ai/pusaman/web/ProblemController.java`

- [ ] **Step 1: 在 ProblemController.labels() 上添加 @RateLimit**

在 `src/main/java/com/lab/ai/pusaman/web/ProblemController.java` 中，给 `labels()` 方法添加注解：

```java
import com.lab.ai.pusaman.annotation.RateLimit;

// 在 @GetMapping("/labels") 下方添加
@RateLimit(qps = 5.0)
```

修改后 labels 方法前的注解为：

```java
@GetMapping("/labels")
@RateLimit(qps = 5.0)
public ResponseEntity<List<ProblemLabel>> labels() {
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw compile -q 2>&1 | tail -10
```

预期：无错误

- [ ] **Step 3: 运行全量测试**

```bash
./mvnw test -q 2>&1 | grep -E "Tests run:|BUILD" | tail -10
```

预期：全部通过，BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/web/ProblemController.java
git commit -m "feat: apply @RateLimit(qps=5.0) to /problem/labels as example"
```

---

### Task 4: 集成验证（手动）

- [ ] **Step 1: 启动应用**

```bash
export ADMIN_TOKEN=test-token
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw spring-boot:run
```

- [ ] **Step 2: 快速连续请求触发限流**

```bash
for i in {1..10}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:7799/problem/labels
done
```

预期：前几次返回 `200`，超过 QPS 后返回 `429`（qps=5.0 时约 200ms 一个令牌，快速连发应触发）

- [ ] **Step 3: 间隔请求正常响应**

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:7799/problem/labels
sleep 1
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:7799/problem/labels
```

预期：两次均返回 `200`
