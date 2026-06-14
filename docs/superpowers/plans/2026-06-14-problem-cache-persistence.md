# ProblemCache 持久化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 每 5 分钟将 ProblemCache 内存数据持久化为本地 JSON 文件，并在服务启动时自动加载该文件恢复 cache。

**Architecture:** 新建 `CacheService` Bean，通过 `@Scheduled` 定时写文件，通过 `@EventListener(ApplicationReadyEvent)` 在启动时读文件。文件路径通过 `application.yml` 配置，Jackson `ObjectMapper` 处理序列化。

**Tech Stack:** Spring Boot 3.5, `@Scheduled`, `ApplicationReadyEvent`, Jackson ObjectMapper

---

### Task 1: 为 ProblemCache 新增 getAll() 方法

**Files:**
- Modify: `src/main/java/com/lab/ai/pusaman/entity/ProblemCache.java`
- Create: `src/test/java/com/lab/ai/pusaman/entity/ProblemCacheTest.java`

- [ ] **Step 1: 创建测试文件并写失败测试**

新建 `src/test/java/com/lab/ai/pusaman/entity/ProblemCacheTest.java`：

```java
package com.lab.ai.pusaman.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProblemCacheTest {

    @BeforeEach
    void clearCache() throws Exception {
        // 通过反射清空静态 cache，保证测试隔离
        var field = ProblemCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        ((java.util.Map<?, ?>) field.get(null)).clear();
    }

    @Test
    void getAll_returnsAllEntries() {
        ProblemCache.addProblem(1, "MySQL问题1");
        ProblemCache.addProblem(1, "MySQL问题2");
        ProblemCache.addProblem(2, "Redis问题1");

        Map<Integer, List<String>> all = ProblemCache.getAll();

        assertEquals(2, all.size());
        assertEquals(List.of("MySQL问题1", "MySQL问题2"), all.get(1));
        assertEquals(List.of("Redis问题1"), all.get(2));
    }

    @Test
    void getAll_returnsUnmodifiableView() {
        ProblemCache.addProblem(1, "问题1");
        Map<Integer, List<String>> all = ProblemCache.getAll();

        assertThrows(UnsupportedOperationException.class, () -> all.put(99, List.of("x")));
    }

    @Test
    void getAll_emptyWhenCacheEmpty() {
        assertTrue(ProblemCache.getAll().isEmpty());
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败（getAll 不存在）**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw test -pl . -Dtest=ProblemCacheTest -q 2>&1 | tail -20
```

预期：编译错误，`cannot find symbol: method getAll()`

- [ ] **Step 3: 在 ProblemCache 中添加 getAll() 方法**

在 `src/main/java/com/lab/ai/pusaman/entity/ProblemCache.java` 末尾，`getProblems` 方法后添加：

```java
    public static Map<Integer, List<String>> getAll() {
        return Collections.unmodifiableMap(cache);
    }
```

同时在文件顶部 import 中确认已有 `import java.util.Map;`（已存在，无需新增）。

完整文件应为：

```java
package com.lab.ai.pusaman.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProblemCache {

    private static final Map<Integer, List<String>> cache = new ConcurrentHashMap<>();

    public static void addProblem(Integer labelId, String problem) {
        cache.computeIfAbsent(labelId, k -> new ArrayList<>()).add(problem);
    }

    public static void addProblem(Integer labelId, List<String> problems) {
        problems.forEach(p -> ProblemCache.addProblem(labelId, p));
    }

    public static List<String> getProblems(Integer labelId) {
        return cache.getOrDefault(labelId, Collections.emptyList());
    }

    public static Map<Integer, List<String>> getAll() {
        return Collections.unmodifiableMap(cache);
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
./mvnw test -pl . -Dtest=ProblemCacheTest -q 2>&1 | tail -10
```

预期：`Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/entity/ProblemCache.java \
        src/test/java/com/lab/ai/pusaman/entity/ProblemCacheTest.java
git commit -m "feat: add ProblemCache.getAll() for persistence"
```

---

### Task 2: 添加配置项并开启 @EnableScheduling

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/java/com/lab/ai/pusaman/PsmApplication.java`

- [ ] **Step 1: 在 application.yml 末尾追加配置**

在 `src/main/resources/application.yml` 末尾新增：

```yaml
pusaman:
  cache:
    file-path: data/problem-cache.json
```

完整文件末尾应为：
```yaml
      embedding.options.model: Embedding-2

pusaman:
  cache:
    file-path: data/problem-cache.json
```

- [ ] **Step 2: 在 PsmApplication 添加 @EnableScheduling**

修改 `src/main/java/com/lab/ai/pusaman/PsmApplication.java`：

```java
package com.lab.ai.pusaman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@EnableScheduling
public class PsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsmApplication.class, args);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -q 2>&1 | tail -10
```

预期：无错误输出

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yml \
        src/main/java/com/lab/ai/pusaman/PsmApplication.java
git commit -m "feat: enable scheduling and add cache file-path config"
```

---

### Task 3: 新建 CacheService

**Files:**
- Create: `src/main/java/com/lab/ai/pusaman/service/CacheService.java`
- Create: `src/test/java/com/lab/ai/pusaman/service/CacheServiceTest.java`

- [ ] **Step 1: 写失败测试**

新建 `src/test/java/com/lab/ai/pusaman/service/CacheServiceTest.java`：

```java
package com.lab.ai.pusaman.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.ai.pusaman.entity.ProblemCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    @TempDir
    Path tempDir;

    CacheService cacheService;

    @BeforeEach
    void setUp() throws Exception {
        // 清空静态 cache
        var field = ProblemCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();

        String filePath = tempDir.resolve("problem-cache.json").toString();
        cacheService = new CacheService(new ObjectMapper(), filePath);
    }

    @Test
    void persistToFile_writesJsonFile() throws IOException {
        ProblemCache.addProblem(1, "MySQL问题1");
        ProblemCache.addProblem(2, "Redis问题1");

        cacheService.persistToFile();

        Path file = tempDir.resolve("problem-cache.json");
        assertTrue(Files.exists(file));
        String content = Files.readString(file);
        assertTrue(content.contains("MySQL问题1"));
        assertTrue(content.contains("Redis问题1"));
    }

    @Test
    void loadFromFile_restoresCacheFromJson() throws IOException {
        // 先写一个 JSON 文件
        Path file = tempDir.resolve("problem-cache.json");
        String json = "{\"1\":[\"MySQL问题1\",\"MySQL问题2\"],\"2\":[\"Redis问题1\"]}";
        Files.writeString(file, json);

        cacheService.loadFromFile();

        assertEquals(List.of("MySQL问题1", "MySQL问题2"), ProblemCache.getProblems(1));
        assertEquals(List.of("Redis问题1"), ProblemCache.getProblems(2));
    }

    @Test
    void loadFromFile_silentlySkipsWhenFileNotExists() {
        // 文件不存在，不应抛出异常
        assertDoesNotThrow(() -> cacheService.loadFromFile());
        assertTrue(ProblemCache.getAll().isEmpty());
    }

    @Test
    void persistToFile_createsParentDirectoriesIfNeeded() throws IOException {
        String nestedPath = tempDir.resolve("a/b/c/problem-cache.json").toString();
        CacheService nestedService = new CacheService(new ObjectMapper(), nestedPath);
        ProblemCache.addProblem(1, "问题1");

        assertDoesNotThrow(() -> nestedService.persistToFile());
        assertTrue(Files.exists(Path.of(nestedPath)));
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败（CacheService 不存在）**

```bash
./mvnw test -pl . -Dtest=CacheServiceTest -q 2>&1 | tail -20
```

预期：编译错误，`cannot find symbol: class CacheService`

- [ ] **Step 3: 实现 CacheService**

新建 `src/main/java/com/lab/ai/pusaman/service/CacheService.java`：

```java
package com.lab.ai.pusaman.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.ai.pusaman.entity.ProblemCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final ObjectMapper objectMapper;
    private final String filePath;

    public CacheService(ObjectMapper objectMapper,
                        @Value("${pusaman.cache.file-path}") String filePath) {
        this.objectMapper = objectMapper;
        this.filePath = filePath;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void persistToFile() {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            objectMapper.writeValue(path.toFile(), ProblemCache.getAll());
            log.info("ProblemCache persisted to {}", filePath);
        } catch (IOException e) {
            log.error("Failed to persist ProblemCache to {}", filePath, e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadFromFile() {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            log.warn("Cache file not found at {}, starting with empty cache", filePath);
            return;
        }
        try {
            Map<Integer, List<String>> data = objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<Map<Integer, List<String>>>() {}
            );
            data.forEach(ProblemCache::addProblem);
            log.info("ProblemCache loaded from {}, {} labels", filePath, data.size());
        } catch (IOException e) {
            log.error("Failed to load ProblemCache from {}, starting with empty cache", filePath, e);
        }
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
./mvnw test -pl . -Dtest=CacheServiceTest -q 2>&1 | tail -10
```

预期：`Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: 运行所有测试**

```bash
./mvnw test -q 2>&1 | tail -15
```

预期：所有测试通过，无 FAIL/ERROR

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/service/CacheService.java \
        src/test/java/com/lab/ai/pusaman/service/CacheServiceTest.java
git commit -m "feat: add CacheService for scheduled persistence and startup loading"
```

---

### Task 4: 端到端验证（手动）

- [ ] **Step 1: 启动应用，验证启动日志**

```bash
./mvnw spring-boot:run 2>&1 | grep -E "Cache file not found|ProblemCache loaded"
```

预期输出（首次启动，文件不存在）：
```
WARN  c.l.a.p.service.CacheService - Cache file not found at data/problem-cache.json, starting with empty cache
```

- [ ] **Step 2: 上传 markdown 文件，触发 cache 写入**

通过 `/doc/upload` 接口上传 markdown 文件，使 ProblemCache 中有数据。

- [ ] **Step 3: 等待定时任务执行（或调低 fixedRate 临时测试）**

等待 5 分钟，或临时将 `CacheService.persistToFile()` 的 `fixedRate` 改为 `5000`（5秒）验证后改回。

验证文件生成：
```bash
cat data/problem-cache.json
```

预期：JSON 文件内容如 `{"1":["MySQL问题1",...],"2":[...]}`

- [ ] **Step 4: 重启应用，验证 cache 加载**

```bash
./mvnw spring-boot:run 2>&1 | grep "ProblemCache loaded"
```

预期：
```
INFO  c.l.a.p.service.CacheService - ProblemCache loaded from data/problem-cache.json, N labels
```

验证接口正常返回数据：
```bash
curl http://localhost:7799/problem/labels
```
