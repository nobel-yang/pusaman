# Admin 认证拦截器与文件删除接口设计

## 目标

1. 对所有 `/admin/**` 请求，拦截并校验 `Authorization: Bearer <token>` header，与环境变量 `ADMIN_TOKEN` 对比，不一致返回 401
2. 新增 `DELETE /admin/file/{fileName}` 接口，删除项目根目录 `data/` 下的指定文件

## 架构

```
请求 /admin/** → AdminAuthInterceptor 校验 token
                  ├─ 通过 → Controller 处理业务
                  └─ 失败 → 401 Unauthorized

DELETE /admin/file/{fileName} → 路径穿越防御 → 删除 data/{fileName}
                                  ├─ 成功 → 200
                                  ├─ 文件不存在 → 404
                                  └─ 删除失败 → 500
```

## 组件变更

### 新建：`config/AdminAuthInterceptor.java`

实现 `HandlerInterceptor`，在 `preHandle` 中：

1. 取 `Authorization` header
2. 校验格式为 `Bearer <token>`（不为空、以 `"Bearer "` 开头）
3. 提取 token，与 `System.getenv("ADMIN_TOKEN")` 对比
4. `ADMIN_TOKEN` 未配置（null 或空）→ 拒绝，返回 401（安全兜底）
5. token 不匹配 → 返回 401，响应体 `Unauthorized`
6. 匹配 → 放行

### 新建：`config/WebConfig.java`

实现 `WebMvcConfigurer`，注入 `AdminAuthInterceptor`，注册到 `/admin/**` 路径。

### 修改：`admin/AdminController.java`

新增方法：

```
DELETE /admin/file/{fileName}
```

逻辑：
- 以项目启动目录为基准，构造 `data/{fileName}` 路径
- 路径穿越防御：`toRealPath()` 后校验必须以 `data/` 目录的规范路径为前缀
- 文件不存在 → 404
- 删除成功 → 200，响应体 `deleted`
- 删除失败（IO 异常等）→ 500

## 错误处理

| 场景 | HTTP 状态 | 响应体 |
|------|-----------|--------|
| `Authorization` header 缺失或格式错误 | 401 | `Unauthorized` |
| token 与 `ADMIN_TOKEN` 不匹配 | 401 | `Unauthorized` |
| `ADMIN_TOKEN` 环境变量未配置 | 401 | `Unauthorized` |
| 路径穿越攻击（`../` 等） | 400 | `Invalid file name` |
| 文件不存在 | 404 | `File not found` |
| 删除失败 | 500 | 异常信息 |

## 安全说明

- 路径穿越防御：使用 `Path.toRealPath()` 或 `normalize()` 后验证路径前缀，防止 `../` 绕过 `data/` 目录限制
- `ADMIN_TOKEN` 未配置时默认拒绝，而非默认放行
- token 比较使用常量时间比较（`MessageDigest.isEqual`）防止时序攻击（可选，当前先用 `equals` 实现）

## 不变更范围

- 现有 `/problem/**` 接口不受影响
- 不引入 Spring Security 依赖
