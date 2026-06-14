# Label 从文件动态加载 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 服务启动时从 `data/label.json` 加载标签数据到内存 map，`/problem/labels` 从 map 读取；新增 `POST /admin/file/label` 接口上传 label.json 并立即热更新。

**Architecture:** 新建 `LabelCache`（静态 ConcurrentHashMap，类比 `ProblemCache`）和 `LabelService`（启动加载 + 保存热更新）；`ProblemController.labels()` 改为读 `LabelCache`；`AdminController` 新增上传接口委托 `LabelService`。

**Tech Stack:** Spring Boot 3.5, `ApplicationReadyEvent`, Jackson ObjectMapper, JUnit 5

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/java/com/lab/ai/pusaman/entity/LabelCache.java` | 新建 | 静态 map 存储，`reload()`、`getAll()` |
| `src/main/java/com/lab/ai/pusaman/service/LabelService.java` | 新建 | 文件加载、保存、热更新 |
| `src/main/java/com/lab/ai/pusaman/admin/AdminController.java` | 修改 | 新增 `POST /admin/file/label` |
| `src/main/java/com/lab/ai/pusaman/web/ProblemController.java` | 修改 | `labels()` 改为读 `LabelCache` |
| `src/test/java/com/lab/ai/pusaman/entity/LabelCacheTest.java` | 新建 | LabelCache 单元测试 |
| `src/test/java/com/lab/ai/pusaman/service/LabelServiceTest.java` | 新建 | LabelService 单元测试 |

---

### Task 1: 实现 LabelCache

**Files:**
- Create: `src/main/java/com/lab/ai/pusaman/entity/LabelCache.java`
- Create: `src/test/java/com/lab/ai/pusaman/entity/LabelCacheTest.java`

- [ ] **Step 1: 写失败测试**

新建 `src/test/java/com/lab/ai/pusaman/entity/LabelCacheTest.java`：

```java
package com.lab.ai.pusaman.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LabelCacheTest {

    @BeforeEach
    void clearCache() throws Exception {
        var field = LabelCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        ((java.util.Map<?, ?>) field.get(null)).clear();
    }

    @Test
    void reload_populatesCache() {
        List<ProblemLabel> labels = List.of(
                ProblemLabel.builder().labelId(1).labelName("MySQL").build(),
                ProblemLabel.builder().labelId(2).labelName("Redis").build()
        );

        LabelCache.reload(labels);

        Map<Integer, String> all = LabelCache.getAll();
        assertEquals(2, all.size());
        assertEquals("MySQL", all.get(1));
        assertEquals("Redis", all.get(2));
    }

    @Test
    void reload_replacesExistingData() {
        LabelCache.reload(List.of(
                ProblemLabel.builder().labelId(1).labelName("MySQL").build()
        ));
        LabelCache.reload(List.of(
                ProblemLabel.builder().labelId(2).labelName("Redis").build()
        ));

        Map<Integer, String> all = LabelCache.getAll();
        assertEquals(1, all.size());
        assertNull(all.get(1));
        assertEquals("Redis", all.get(2));
    }

    @Test
    void getAll_returnsUnmodifiableMap() {
        LabelCache.reload(List.of(
                ProblemLabel.builder().labelId(1).labelName("MySQL").build()
        ));
        Map<Integer, String> all = LabelCache.getAll();

        assertThrows(UnsupportedOperationException.class, () -> all.put(99, "test"));
    }

    @Test
    void getAll_emptyWhenCacheEmpty() {
        assertTrue(LabelCache.getAll().isEmpty());
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw test -Dtest=LabelCacheTest -q 2>&1 | tail -20
```

预期：编译错误，`cannot find symbol: class LabelCache`

- [ ] **Step 3: 实现 LabelCache**

新建 `src/main/java/com/lab/ai/pusaman/entity/LabelCache.java`：

```java
package com.lab.ai.pusaman.entity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LabelCache {

    private static final Map<Integer, String> cache = new ConcurrentHashMap<>();

    public static void reload(List<ProblemLabel> labels) {
        cache.clear();
        labels.forEach(l -> cache.put(l.getLabelId(), l.getLabelName()));
    }

    public static Map<Integer, String> getAll() {
        return Map.copyOf(cache);
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
./mvnw test -Dtest=LabelCacheTest -q 2>&1 | tail -10
```

预期：`Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/entity/LabelCache.java \
        src/test/java/com/lab/ai/pusaman/entity/LabelCacheTest.java
git commit -m "feat: add LabelCache for dynamic label storage"
```

---

### Task 2: 实现 LabelService

**Files:**
- Create: `src/main/java/com/lab/ai/pusaman/service/LabelService.java`
- Create: `src/test/java/com/lab/ai/pusaman/service/LabelServiceTest.java`

- [ ] **Step 1: 写失败测试**

新建 `src/test/java/com/lab/ai/pusaman/service/LabelServiceTest.java`：

```java
package com.lab.ai.pusaman.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.ai.pusaman.entity.LabelCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LabelServiceTest {

    @TempDir
    Path tempDir;

    LabelService labelService;

    @BeforeEach
    void setUp() throws Exception {
        // 清空 LabelCache
        var field = LabelCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();

        String filePath = tempDir.resolve("label.json").toString();
        labelService = new LabelService(new ObjectMapper(), filePath);
    }

    @Test
    void loadFromFile_populatesCache() throws IOException {
        Path file = tempDir.resolve("label.json");
        String json = "[{\"labelId\":1,\"labelName\":\"MySQL\"},{\"labelId\":2,\"labelName\":\"Redis\"}]";
        Files.writeString(file, json);

        labelService.loadFromFile();

        assertEquals("MySQL", LabelCache.getAll().get(1));
        assertEquals("Redis", LabelCache.getAll().get(2));
    }

    @Test
    void loadFromFile_silentlySkipsWhenFileNotExists() {
        assertDoesNotThrow(() -> labelService.loadFromFile());
        assertTrue(LabelCache.getAll().isEmpty());
    }

    @Test
    void saveAndReload_writesFileAndUpdatesCache() throws IOException {
        byte[] content = "[{\"labelId\":1,\"labelName\":\"MySQL\"}]".getBytes();

        labelService.save(content);

        Path file = tempDir.resolve("label.json");
        assertTrue(Files.exists(file));
        assertEquals("MySQL", LabelCache.getAll().get(1));
    }

    @Test
    void saveAndReload_throwsOnInvalidJson() {
        byte[] invalid = "not-valid-json".getBytes();

        assertThrows(IllegalArgumentException.class, () -> labelService.save(invalid));
        assertTrue(LabelCache.getAll().isEmpty());
    }

    @Test
    void saveAndReload_throwsWhenNotArray() {
        byte[] notArray = "{\"labelId\":1,\"labelName\":\"MySQL\"}".getBytes();

        assertThrows(IllegalArgumentException.class, () -> labelService.save(notArray));
        assertTrue(LabelCache.getAll().isEmpty());
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败**

```bash
./mvnw test -Dtest=LabelServiceTest -q 2>&1 | tail -20
```

预期：编译错误，`cannot find symbol: class LabelService`

- [ ] **Step 3: 实现 LabelService**

新建 `src/main/java/com/lab/ai/pusaman/service/LabelService.java`：

```java
package com.lab.ai.pusaman.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.ai.pusaman.entity.LabelCache;
import com.lab.ai.pusaman.entity.ProblemLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class LabelService {

    private static final Logger log = LoggerFactory.getLogger(LabelService.class);

    private final ObjectMapper objectMapper;
    private final String filePath;

    public LabelService(ObjectMapper objectMapper,
                        @Value("${pusaman.label.file-path:data/label.json}") String filePath) {
        this.objectMapper = objectMapper;
        this.filePath = filePath;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadFromFile() {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            log.warn("Label file not found at {}, starting with empty labels", filePath);
            return;
        }
        try {
            List<ProblemLabel> labels = objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<List<ProblemLabel>>() {}
            );
            LabelCache.reload(labels);
            log.info("Labels loaded from {}, {} entries", filePath, labels.size());
        } catch (IOException e) {
            log.error("Failed to load labels from {}", filePath, e);
        }
    }

    public void saveAndReload(byte[] content) throws IOException {
        List<ProblemLabel> labels;
        try {
            labels = objectMapper.readValue(content, new TypeReference<List<ProblemLabel>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid label JSON: " + e.getMessage(), e);
        }

        Path path = Path.of(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, content);
        LabelCache.reload(labels);
        log.info("Labels saved to {} and reloaded, {} entries", filePath, labels.size());
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
./mvnw test -Dtest=LabelServiceTest -q 2>&1 | tail -10
```

预期：`Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: 运行全量测试**

```bash
./mvnw test -q 2>&1 | grep -E "Tests run:|BUILD" | tail -10
```

预期：全部通过，BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/service/LabelService.java \
        src/test/java/com/lab/ai/pusaman/service/LabelServiceTest.java
git commit -m "feat: add LabelService for label file loading and hot reload"
```

---

### Task 3: 新增 POST /admin/file/label 接口

**Files:**
- Modify: `src/main/java/com/lab/ai/pusaman/admin/AdminController.java`

- [ ] **Step 1: 修改 AdminController，注入 LabelService 并新增接口**

当前 `AdminController.java` 完整内容如下（只读参考，不要修改此步骤）：

```
@Resource MarkdownFileUploadApplication markdownFileUploadApplication
@PutMapping("/doc/upload") upload()
@DeleteMapping("/file/{fileName}") deleteFile()
```

将 `src/main/java/com/lab/ai/pusaman/admin/AdminController.java` 修改为（完整文件）：

```java
package com.lab.ai.pusaman.admin;

import com.lab.ai.pusaman.application.FileUploadApplication;
import com.lab.ai.pusaman.entity.MarkdownFile;
import com.lab.ai.pusaman.service.LabelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    FileUploadApplication markdownFileUploadApplication;

    @Resource
    LabelService labelService;

    @PutMapping("/doc/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            log.info("File upload request:{}", fileName);
            Objects.requireNonNull(fileName);
            if (!fileName.contains(".md")) {
                throw new IllegalArgumentException("Supported markdown only.");
            }

            markdownFileUploadApplication.upload(
                    MarkdownFile.builder()
                            .fileName(fileName)
                            .file(file)
                            .build()
            );
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/file/label")
    public ResponseEntity<String> uploadLabel(@RequestParam("file") MultipartFile file) {
        try {
            labelService.save(file.getBytes());
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.error("Failed to save label file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/file/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            if (Path.of(fileName).isAbsolute()) {
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            Path base = Path.of("data").toAbsolutePath().normalize();
            Path target = base.resolve(fileName).normalize();

            if (!target.startsWith(base)) {
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            if (!Files.exists(target)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            Files.delete(target);
            log.info("Deleted file: {}", target);
            return ResponseEntity.ok("deleted");
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
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
git add src/main/java/com/lab/ai/pusaman/admin/AdminController.java
git commit -m "feat: add POST /admin/file/label to upload and hot-reload labels"
```

---

### Task 4: 修改 ProblemController 从 LabelCache 读取

**Files:**
- Modify: `src/main/java/com/lab/ai/pusaman/web/ProblemController.java`

- [ ] **Step 1: 修改 labels() 方法**

将 `src/main/java/com/lab/ai/pusaman/web/ProblemController.java` 的 `labels()` 方法替换为（只修改这一个方法）：

```java
@GetMapping("/labels")
public ResponseEntity<List<ProblemLabel>> labels() {
    return ResponseEntity.ok(
            LabelCache.getAll().entrySet().stream()
                    .map(e -> ProblemLabel.builder()
                            .labelId(e.getKey())
                            .labelName(e.getValue())
                            .build()
                    ).collect(Collectors.toList())
    );
}
```

同时在文件顶部 import 中增加（如未有）：
```java
import com.lab.ai.pusaman.entity.LabelCache;
```

并删除不再使用的 import：

```java


```

完整修改后的 `ProblemController.java`：

```java
package com.lab.ai.pusaman.web;

import com.lab.ai.pusaman.application.ProblemGenApplication;
import com.lab.ai.pusaman.application.QuestionSearchApplication;
import com.lab.ai.pusaman.entity.LabelCache;
import com.lab.ai.pusaman.entity.ProblemLabel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/problem")
public class ProblemController {

    @Resource
    QuestionSearchApplication questionSearchApplication;
    @Resource
    ProblemGenApplication problemGenApplication;

    @GetMapping("/labels")
    public ResponseEntity<List<ProblemLabel>> labels() {
        return ResponseEntity.ok(
                LabelCache.getAll().entrySet().stream()
                        .map(e -> ProblemLabel.builder()
                                .labelId(e.getKey())
                                .labelName(e.getValue())
                                .build()
                        ).collect(Collectors.toList())
        );
    }

    @GetMapping("/{labelId}/next")
    public ResponseEntity<String> next(@PathVariable Integer labelId) {
        log.info("Next problem label:{}", labelId);
        return ResponseEntity.ok(problemGenApplication.genNextProblem(labelId));
    }

    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestParam(value = "q") String question,
                            @RequestParam(value = "a") String answer) {
        log.info("Question: {}, answer from user:{}", question, answer);
        return questionSearchApplication.searchAnswer(question, answer);
    }
}
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
git commit -m "feat: replace LabelEnum with LabelCache in /problem/labels"
```

---

### Task 5: 端到端验证（手动）

- [ ] **Step 1: 准备 label.json 文件**

```bash
cat > /tmp/label.json << 'EOF'
[
  {"labelId": 1, "labelName": "MySQL"},
  {"labelId": 2, "labelName": "Redis"},
  {"labelId": 3, "labelName": "Kafka"}
]
EOF
```

- [ ] **Step 2: 将文件放入 data/ 目录并启动应用**

```bash
mkdir -p /Users/yangkegang/workspace/ai-lab/pusaman/data
cp /tmp/label.json /Users/yangkegang/workspace/ai-lab/pusaman/data/label.json
export ADMIN_TOKEN=test-token
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw spring-boot:run 2>&1 | grep -E "Labels loaded|Label file not found|started"
```

预期启动日志包含：
```
INFO  c.l.a.p.service.LabelService - Labels loaded from data/label.json, 3 entries
```

- [ ] **Step 3: 验证 /problem/labels 返回正确数据**

```bash
curl -s http://localhost:7799/problem/labels | python3 -m json.tool
```

预期：返回包含 MySQL、Redis、Kafka 的数组

- [ ] **Step 4: 上传新的 label.json，验证热更新**

```bash
cat > /tmp/label-new.json << 'EOF'
[
  {"labelId": 1, "labelName": "MySQL"},
  {"labelId": 2, "labelName": "Redis"},
  {"labelId": 10, "labelName": "系统设计"}
]
EOF

curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer test-token" \
  -X POST http://localhost:7799/admin/file/label \
  -F "file=@/tmp/label-new.json"
```

预期：`success\n200`

- [ ] **Step 5: 验证热更新生效（无需重启）**

```bash
curl -s http://localhost:7799/problem/labels | python3 -m json.tool
```

预期：返回包含 MySQL、Redis、系统设计（Kafka 消失，系统设计出现）

- [ ] **Step 6: 验证上传无效 JSON 返回 400**

```bash
curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer test-token" \
  -X POST http://localhost:7799/admin/file/label \
  -F "file=@/tmp/label.json" \
  --data-urlencode ""
```

或直接用无效内容：
```bash
echo "not-valid" > /tmp/invalid.json
curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer test-token" \
  -X POST http://localhost:7799/admin/file/label \
  -F "file=@/tmp/invalid.json"
```

预期：`400`
