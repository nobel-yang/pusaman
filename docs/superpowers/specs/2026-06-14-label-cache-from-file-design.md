# Label 从文件动态加载设计

## 目标

1. 服务启动时从 `data/label.json` 加载标签数据到内存 map，`/problem/labels` 从 map 读取（替代原有 `LabelEnum` 遍历）
2. 新增 `POST /admin/file/label` 接口，上传 `label.json` 文件，存入 `data/label.json` 并立即热更新内存 map

## 架构

```
启动时: data/label.json → LabelService.loadFromFile() → LabelCache.reload()
读取时: GET /problem/labels → ProblemController → LabelCache.getAll() → 返回列表
上传时: POST /admin/file/label → AdminController → LabelService.saveAndReload() → 写文件 + LabelCache.reload()
```

## 数据格式

`data/label.json`：
```json
[
  {"labelId": 1, "labelName": "MySQL"},
  {"labelId": 2, "labelName": "Redis"}
]
```

## 组件变更

### 新建：`entity/LabelCache.java`

- 静态 `ConcurrentHashMap<Integer, String>` 存储 labelId → labelName
- `reload(List<ProblemLabel>)`：清空后批量写入（原子替换）
- `getAll()`：返回不可变副本 `Map.copyOf(cache)`

### 新建：`service/LabelService.java`

- `@EventListener(ApplicationReadyEvent.class)` 的 `loadFromFile()`：
  - 读取 `data/label.json`，反序列化为 `List<ProblemLabel>`
  - 调用 `LabelCache.reload()`
  - 文件不存在 → WARN 日志，cache 保持空
  - 格式错误 → ERROR 日志，cache 保持空
- `saveAndReload(byte[] content)`：
  - 先验证内容是合法的 `List<ProblemLabel>` JSON（不合法抛 `IllegalArgumentException`）
  - 写入 `data/label.json`（自动创建父目录）
  - 调用 `LabelCache.reload()` 热更新

### 修改：`admin/AdminController.java`

新增：
```
POST /admin/file/label
Content-Type: multipart/form-data，参数名 "file"
```

逻辑：
- 读取文件字节，委托 `LabelService.saveAndReload()`
- 成功 → 200，响应体 `"success"`
- 格式错误 → 400，响应体错误信息
- 其他异常 → 500

### 修改：`web/ProblemController.java`

`labels()` 改为：
```java
LabelCache.getAll().entrySet().stream()
    .map(e -> ProblemLabel.builder().labelId(e.getKey()).labelName(e.getValue()).build())
    .collect(Collectors.toList())
```

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 启动时文件不存在 | WARN 日志，cache 为空，`/labels` 返回 `[]` |
| 启动时文件格式错误 | ERROR 日志，cache 为空 |
| 上传文件不是合法 JSON 数组 | 400，响应体包含错误信息 |
| 上传时写文件失败 | 500 |

## 不变更范围

- `LabelEnum` 保留，不删除（其他地方可能引用）
- 现有 `ProblemCache`、`CacheService` 不受影响
- `data/label.json` 路径硬编码为 `data/label.json`，不通过配置暴露
