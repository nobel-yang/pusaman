# [Pusaman（菩萨蛮）](https://pusaman.xyz)

## 📖 1. 概述

[Pusaman](https://pusaman.xyz) 是一个基于 AI 的知识复习应用。用户选择知识标签后，系统随机推送不重复的题目；用户作答后，系统通过 RAG（检索增强生成）检索相关知识文档，调用大语言模型对用户答案进行评判并给出参考答案，整个过程以 SSE 流式输出，实时展示评判结果。

🛠️ **技术栈：**

- 后端：Spring Boot 3.5 + Spring AI 1.1 + ZhiPu AI（智谱大模型）
- 向量数据库：Milvus
- 前端：原生 HTML/CSS/JS + Marked.js
- Java 26 + Lombok + Guava

## 🧩 2. 功能模块

### 🏷️ 2.1 标签管理

- 从 `data/label.json` 加载知识标签
- 管理员可通过 `/admin/file/label` 上传新的标签文件，热更新内存缓存

### 📝 2.2 题目生成与推送

- 管理员上传 Markdown 知识文档后，自动提取标题作为题目，存入 `ProblemCache`
- 根据 MD5 哈希去重，同一标签下随机推送未做过的题目
- 所有题目做过后自动重置，循环出题

### 🤖 2.3 RAG 智能评判

- 用户提交答案后，系统在 Milvus 中进行向量相似度搜索，检索相关文档片段
- 将题目、用户答案和检索到的上下文拼接为 Prompt，调用 ZhiPu AI 流式生成评判结果
- 返回结构化 JSON：`j`（系统评判）和 `r`（参考答案）
- 前端通过 SSE 实时展示流式输出，完成后以 Markdown 渲染最终结果

### 🔐 2.4 管理后台

- Bearer Token 鉴权，拦截所有 `/admin/**` 请求
- 上传 Markdown 文档：自动提取题目 + 文档向量化入库
- 上传标签 JSON：持久化并热更新标签缓存
- 删除数据文件
- 查看当前题目缓存

### ⚡ 2.5 基础设施

- **限流**：基于 AOP + Guava RateLimiter 的注解式限流（`@RateLimit`），支持按方法独立配置 QPS
- **缓存持久化**：`ProblemCache` 每 60 秒自动写入 `data/problem-cache.json`，启动时自动加载
- **健康检查**：`GET /health` 端点

## 🏗️ 3. 项目结构与架构

### 📁 3.1 目录结构

```
pusaman/
├── pom.xml                                    # Maven 依赖配置
├── docs/
│   ├── plans/                                 # 实现计划文档
│   └── specs/                                 # 设计规格文档
└── src/main/
    ├── java/com/lab/ai/pusaman/
    │   ├── PsmApplication.java                # 启动类 + 健康检查
    │   ├── admin/
    │   │   └── AdminController.java           # 管理后台 REST API
    │   ├── annotation/
    │   │   ├── RateLimit.java                 # 限流注解
    │   │   └── RateLimitAspect.java           #  限流切面
    │   ├── application/
    │   │   ├── FileUploadApplication.java     # 文件上传编排（提取题目 + 向量化）
    │   │   ├── ProblemGenApplication.java     # 题目生成 + 格式化
    │   │   └── QuestionSearchApplication.java # RAG 检索 + LLM 评判编排
    │   ├── config/
    │   │   ├── AdminAuthInterceptor.java      # 管理后台 Bearer Token 鉴权
    │   │   ├── RagConfig.java                 # ChatClient + 系统提示词配置
    │   │   └── WebConfig.java                 # 拦截器注册
    │   ├── entity/
    │   │   ├── JsonFile.java                  # JSON 文件上传 DTO
    │   │   ├── LabelCache.java                # 标签内存缓存（ConcurrentHashMap）
    │   │   ├── MarkdownFile.java              # Markdown 文件上传 DTO
    │   │   ├── Problem.java                   # 题目实体
    │   │   ├── ProblemCache.java              # 题目内存缓存（ConcurrentHashMap）
    │   │   ├── ProblemLabel.java              # 标签实体
    │   │   └── ProblemLabelVo.java            # 标签 VO（含题目数量）
    │   ├── service/
    │   │   ├── CacheService.java              # 缓存定时持久化 + 启动加载
    │   │   ├── EmbeddingService.java          # Markdown → Document → Milvus 向量化
    │   │   ├── LLMService.java                # ChatClient 流式调用封装
    │   │   ├── LabelService.java              # 标签文件读写 + 热加载
    │   │   ├── MarkdownTitleExtractService.java # Markdown 标题提取
    │   │   ├── ProblemGenService.java         # 随机不重复出题
    │   │   └── SearchService.java             # Milvus 向量相似度搜索
    │   ├── util/
    │   │   ├── JsonUtils.java                 # JSON 序列化工具
    │   │   └── Md5Utils.java                  # MD5 哈希工具
    │   └── web/
    │       └── ProblemController.java         # 公开 REST API（题目推送 + SSE 评判）
    └── resources/
        ├── application.yml                    # 应用配置
        └── static/                            # 前端静态文件
            ├── index.html
            ├── style.css
            └── app.js
```

### 🧱 3.2 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                     🌐 前端 (SPA)                            │
│  index.html + app.js + style.css + Marked.js                │
│  标签选择 → 题目展示 → 作答 → SSE 流式接收评判结果           │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / SSE
┌──────────────────────────▼──────────────────────────────────┐
│                🖥️ Web 层 (Controller)                        │
│  ProblemController    → 公开 API（/problem/**）              │
│  AdminController      → 管理 API（/admin/**, Token 鉴权）    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              🧩 Application 层 (编排)                        │
│  FileUploadApplication    → 上传文件 → 提取题目 + 向量化     │
│  ProblemGenApplication    → 出题 + 格式化（去粗体、加标题）   │
│  QuestionSearchApplication→ RAG 检索 + LLM 评判编排          │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│               ⚙️ Service 层 (核心逻辑)                      │
│  ProblemGenService        → 随机不重复出题（MD5 去重）       │
│  SearchService            → Milvus 向量搜索（TopK=5）        │
│  LLMService               → ChatClient 流式调用              │
│  EmbeddingService         → 文档向量化入库                   │
│  CacheService             → 缓存持久化（60s 定时 + 启动加载）│
│  LabelService             → 标签文件管理                     │
│  MarkdownTitleExtractService → Markdown 标题提取            │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│             🏗️ 基础设施 / 数据层                             │
│  LabelCache / ProblemCache  → ConcurrentHashMap 内存缓存     │
│  Milvus VectorStore         → 向量存储与检索                 │
│  ZhiPu AI (ChatClient)      → 大模型推理                     │
│  data/*.json                → 文件持久化                     │
└─────────────────────────────────────────────────────────────┘
```

### 🔄 3.3 核心流程

**📌 出题流程：**
```
用户选择标签 → GET /problem/{labelId}/next
    → ProblemGenApplication.genNextProblem()
        → ProblemGenService.genNextProblem()  // 随机选题 + MD5 去重
        → 格式化（去 ** 粗体，数字前缀转 ### 标题）
    ← 返回题目纯文本
```

**🤖 评判流程：**
```
用户提交答案 → GET /problem/ask?q=...&a=... (SSE)
    → QuestionSearchApplication.searchAnswer()
        → SearchService.search(question)      // Milvus 向量检索 Top5
        → 拼接 Prompt（题目 + 用户答案 + 检索上下文）
        → LLMService.ask(prompt)              // ChatClient 流式生成
    ← 📡 SSE 流式返回 JSON { j: 评判, r: 参考答案 }
```

### 🔑 3.4 环境变量

| 变量 | 说明 |
|------|------|
| `SERVER_PORT` | 🚪 服务端口 |
| `MILVUS_ID` | 🗄️ Milvus 主机地址 |
| `MILVUS_USERNAME` | 👤 Milvus 用户名 |
| `MILVUS_PASSWORD` | 🔒 Milvus 密码 |
| `ZHIPU_APPKEY` | 🔑 智谱 AI API Key |
| `ZHIPU_MODEL_CHAT` | 💬 智谱对话模型名称 |
| `ZHIPU_MODEL_EMBEDDING` | 🔢 智谱嵌入模型名称 |
| `ADMIN_TOKEN` | 🛡️ 管理后台鉴权 Token |

## 📜 开源协议

本项目基于 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html) 协议开源。

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg" alt="License: GPL v3">
</p>
