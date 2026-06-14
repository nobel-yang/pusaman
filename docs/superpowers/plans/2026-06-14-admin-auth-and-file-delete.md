# Admin 认证拦截器与文件删除接口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为所有 `/admin/**` 接口添加 Bearer token 认证拦截器，并新增 `DELETE /admin/file/{fileName}` 接口用于删除 `data/` 目录下的指定文件。

**Architecture:** 新建 `AdminAuthInterceptor` 实现 `HandlerInterceptor`，在新建的 `WebConfig`（实现 `WebMvcConfigurer`）中注册到 `/admin/**` 路径；在现有 `AdminController` 中新增 DELETE 接口，含路径穿越防御。

**Tech Stack:** Spring Boot 3.5, `HandlerInterceptor`, `WebMvcConfigurer`, JUnit 5, MockMvc

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/java/com/lab/ai/pusaman/config/AdminAuthInterceptor.java` | 新建 | Bearer token 校验逻辑 |
| `src/main/java/com/lab/ai/pusaman/config/WebConfig.java` | 新建 | 将拦截器注册到 `/admin/**` |
| `src/main/java/com/lab/ai/pusaman/admin/AdminController.java` | 修改 | 新增 DELETE `/admin/file/{fileName}` |
| `src/test/java/com/lab/ai/pusaman/config/AdminAuthInterceptorTest.java` | 新建 | 拦截器单元测试 |
| `src/test/java/com/lab/ai/pusaman/admin/AdminControllerFileDeleteTest.java` | 新建 | 文件删除接口测试 |

---

### Task 1: 实现 AdminAuthInterceptor

**Files:**
- Create: `src/main/java/com/lab/ai/pusaman/config/AdminAuthInterceptor.java`
- Create: `src/test/java/com/lab/ai/pusaman/config/AdminAuthInterceptorTest.java`

- [ ] **Step 1: 写失败测试**

新建 `src/test/java/com/lab/ai/pusaman/config/AdminAuthInterceptorTest.java`：

```java
package com.lab.ai.pusaman.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuthInterceptorTest {

    AdminAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AdminAuthInterceptor();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("ADMIN_TOKEN");
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    @Test
    void allows_request_when_token_matches() throws Exception {
        // 通过系统属性模拟环境变量（测试替身）
        // 实际实现通过 getAdminToken() 方法读取，测试用 spy 覆盖
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertTrue(spy.preHandle(requestWithBearer("secret123"), res, new Object()));
        assertEquals(200, res.getStatus());
    }

    @Test
    void rejects_when_token_mismatch() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(requestWithBearer("wrong"), res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_authorization_header_missing() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(new MockHttpServletRequest(), res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_no_bearer_prefix() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "secret123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(req, res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_admin_token_env_not_configured() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return null; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(requestWithBearer("anything"), res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_admin_token_env_is_empty() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return ""; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(requestWithBearer("anything"), res, new Object()));
        assertEquals(401, res.getStatus());
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw test -Dtest=AdminAuthInterceptorTest -q 2>&1 | tail -20
```

预期：编译错误，`cannot find symbol: class AdminAuthInterceptor`

- [ ] **Step 3: 实现 AdminAuthInterceptor**

新建 `src/main/java/com/lab/ai/pusaman/config/AdminAuthInterceptor.java`：

```java
package com.lab.ai.pusaman.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String adminToken = getAdminToken();
        if (adminToken == null || adminToken.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return false;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (!adminToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return false;
        }

        return true;
    }

    protected String getAdminToken() {
        return System.getenv("ADMIN_TOKEN");
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
./mvnw test -Dtest=AdminAuthInterceptorTest -q 2>&1 | tail -10
```

预期：`Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/config/AdminAuthInterceptor.java \
        src/test/java/com/lab/ai/pusaman/config/AdminAuthInterceptorTest.java
git commit -m "feat: add AdminAuthInterceptor for /admin/** Bearer token auth"
```

---

### Task 2: 注册拦截器到 /admin/**

**Files:**
- Create: `src/main/java/com/lab/ai/pusaman/config/WebConfig.java`

- [ ] **Step 1: 创建 WebConfig**

新建 `src/main/java/com/lab/ai/pusaman/config/WebConfig.java`：

```java
package com.lab.ai.pusaman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**");
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw compile -q 2>&1 | tail -10
```

预期：无错误输出

- [ ] **Step 3: 运行全量测试**

```bash
./mvnw test -q 2>&1 | grep -E "Tests run:|BUILD" | tail -10
```

预期：全部通过，BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/config/WebConfig.java
git commit -m "feat: register AdminAuthInterceptor for /admin/** via WebConfig"
```

---

### Task 3: 新增 DELETE /admin/file/{fileName} 接口

**Files:**
- Modify: `src/main/java/com/lab/ai/pusaman/admin/AdminController.java`
- Create: `src/test/java/com/lab/ai/pusaman/admin/AdminControllerFileDeleteTest.java`

- [ ] **Step 1: 写失败测试**

新建 `src/test/java/com/lab/ai/pusaman/admin/AdminControllerFileDeleteTest.java`：

```java
package com.lab.ai.pusaman.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllerFileDeleteTest {

    @TempDir
    Path tempDir;

    AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(tempDir.toString());
    }

    @Test
    void deleteFile_returns200_whenFileExists() throws IOException {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, "content");

        var response = controller.deleteFile("test.json");

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(Files.exists(file));
    }

    @Test
    void deleteFile_returns404_whenFileNotExists() {
        var response = controller.deleteFile("nonexistent.json");

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void deleteFile_returns400_whenPathTraversal() {
        var response = controller.deleteFile("../secret.txt");

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void deleteFile_returns400_whenAbsolutePath() {
        var response = controller.deleteFile("/etc/passwd");

        assertEquals(400, response.getStatusCodeValue());
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败**

```bash
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw test -Dtest=AdminControllerFileDeleteTest -q 2>&1 | tail -20
```

预期：编译错误（`AdminController` 没有接受 `String` 参数的构造函数，也没有 `deleteFile` 方法）

- [ ] **Step 3: 修改 AdminController，新增构造函数和 deleteFile 方法**

将 `src/main/java/com/lab/ai/pusaman/admin/AdminController.java` 修改为：

```java
package com.lab.ai.pusaman.admin;

import com.lab.ai.pusaman.application.FileUploadApplication;
import com.lab.ai.pusaman.entity.MarkdownFile;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author yang.nobel
 * @since 2026-06-12 14:25
 **/
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    FileUploadApplication markdownFileUploadApplication;

    private final String dataDir;

    public AdminController() {
        this.dataDir = "data";
    }

    public AdminController(String dataDir) {
        this.dataDir = dataDir;
    }

    @PostMapping("/doc/upload")
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

    @DeleteMapping("/file/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            Path base = Path.of(dataDir).toAbsolutePath().normalize();
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

- [ ] **Step 4: 运行 AdminControllerFileDeleteTest，确认通过**

```bash
./mvnw test -Dtest=AdminControllerFileDeleteTest -q 2>&1 | tail -10
```

预期：`Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: 运行全量测试**

```bash
./mvnw test -q 2>&1 | grep -E "Tests run:|BUILD" | tail -10
```

预期：所有测试通过，BUILD SUCCESS（共 13 个测试）

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lab/ai/pusaman/admin/AdminController.java \
        src/test/java/com/lab/ai/pusaman/admin/AdminControllerFileDeleteTest.java
git commit -m "feat: add DELETE /admin/file/{fileName} with path traversal guard"
```

---

### Task 4: 集成验证（手动）

- [ ] **Step 1: 设置环境变量并启动应用**

```bash
export ADMIN_TOKEN=test-token
cd /Users/yangkegang/workspace/ai-lab/pusaman
./mvnw spring-boot:run
```

- [ ] **Step 2: 验证无 token 时返回 401**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:7799/admin/doc/upload
```

预期：`401`

- [ ] **Step 3: 验证错误 token 时返回 401**

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer wrong-token" \
  http://localhost:7799/admin/doc/upload
```

预期：`401`

- [ ] **Step 4: 验证正确 token 时可通过拦截器（业务层可能报其他错误，但不是 401）**

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer test-token" \
  -X POST http://localhost:7799/admin/doc/upload
```

预期：非 `401`（可能是 `400` 或 `500`，因为没有上传文件）

- [ ] **Step 5: 验证文件删除接口**

```bash
# 先在 data/ 目录创建一个测试文件
mkdir -p data && echo "test" > data/test.txt

# 删除该文件
curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer test-token" \
  -X DELETE http://localhost:7799/admin/file/test.txt
```

预期：响应体 `deleted`，HTTP 状态 `200`

- [ ] **Step 6: 验证路径穿越被拒绝**

```bash
curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer test-token" \
  -X DELETE "http://localhost:7799/admin/file/..%2Fapplication.yml"
```

预期：`400`

- [ ] **Step 7: 验证 /problem 接口不受影响**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:7799/problem/labels
```

预期：`200`（无需 token）
