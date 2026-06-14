# ProblemCache 持久化设计

## 目标

1. 定时（每 5 分钟）将 `ProblemCache` 内存数据写入本地 JSON 文件
2. Spring 服务启动时自动加载该 JSON 文件，恢复 cache 内容

## 架构

```
启动时: JSON文件 → CacheService.loadFromFile() → ProblemCache.addProblem()
运行时: ProblemCache.getAll() → CacheService.persistToFile() → JSON文件（每5分钟）
```

## 组件变更

### 新建：`service/CacheService.java`

- `@Service`，注入 `ObjectMapper` 和配置路径
- `loadFromFile()`：监听 `ApplicationReadyEvent`，读取 JSON 文件反序列化后写入 `ProblemCache`；文件不存在时打印 WARN 日志，静默跳过
- `persistToFile()`：`@Scheduled(fixedRate = 300_000)`，将 `ProblemCache.getAll()` 序列化写入文件；父目录不存在时自动创建

### 修改：`entity/ProblemCache.java`

新增静态方法：
```java
public static Map<Integer, List<String>> getAll() {
    return Collections.unmodifiableMap(cache);
}
```

### 修改：`PsmApplication.java`

新增注解：
```java
@EnableScheduling
```

### 修改：`application.yml`

新增配置：
```yaml
pusaman:
  cache:
    file-path: data/problem-cache.json
```

## JSON 格式

```json
{
  "1": ["MySQL问题1", "MySQL问题2"],
  "2": ["Redis问题1", "Redis问题2"]
}
```

key 为 `Integer` labelId（序列化为字符串），value 为问题列表。

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 启动时文件不存在 | WARN 日志，跳过，cache 为空 |
| 启动时文件损坏/格式错误 | ERROR 日志，跳过，cache 为空 |
| 持久化时目录不存在 | 自动创建父目录 |
| 持久化时写文件失败 | ERROR 日志，不影响主流程 |

## 依赖

无需新增依赖，Jackson（`ObjectMapper`）已通过 `spring-boot-starter-web` 引入。
