# 知识复习页面 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个原生 HTML/JS 单页面应用，支持按标签选题、手写答案、SSE 流式查看 AI 解答，题目和答案均支持 Markdown 渲染。

**Architecture:** 单个 `index.html` 承载两个视图（标签选择 / 问答），通过 `display` 切换。`style.css` 负责样式，`app.js` 负责全部逻辑（视图切换、API 请求、SSE 处理、Markdown 渲染）。

**Tech Stack:** 原生 HTML5 / CSS3 / ES6+ JavaScript，marked.js（CDN），无构建工具。

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `index.html` | 两个视图的 HTML 骨架，引入 CSS / JS |
| `style.css` | 全局样式：布局、卡片、按钮、chip、Markdown 样式 |
| `app.js` | 视图切换、API 调用、SSE 处理、Markdown 渲染 |

---

### Task 1: 创建 index.html 骨架

**Files:**
- Create: `index.html`

- [ ] **Step 1: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>知识复习</title>
  <link rel="stylesheet" href="style.css">
  <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
</head>
<body>

  <!-- 视图1：标签选择 -->
  <div id="view-labels" class="view">
    <div class="container container--labels">
      <h1 class="page-title">选择复习标签</h1>
      <div id="label-grid" class="label-grid"></div>
      <div id="label-error" class="error-msg hidden">
        加载失败，<button id="btn-retry-labels" class="btn-link">重试</button>
      </div>
      <div class="start-bar">
        <button id="btn-start" class="btn btn--primary" disabled>开始复习</button>
      </div>
    </div>
  </div>

  <!-- 视图2：问答 -->
  <div id="view-quiz" class="view hidden">
    <div class="container container--quiz">
      <div class="quiz-topbar">
        <button id="btn-back" class="btn-link">← 返回</button>
        <span id="quiz-label-name" class="topbar-label"></span>
      </div>
      <div id="question-card" class="card markdown-body"></div>
      <div id="quiz-chip" class="chip"></div>
      <textarea id="answer-input" class="answer-input" placeholder="写下你的答案..."></textarea>
      <div class="btn-row">
        <button id="btn-submit" class="btn btn--primary">提交</button>
        <button id="btn-next" class="btn btn--secondary">下一个</button>
      </div>
      <div id="answer-area" class="answer-area hidden">
        <div class="answer-label">AI 答案</div>
        <div id="answer-content" class="markdown-body"></div>
      </div>
    </div>
  </div>

  <script src="app.js"></script>
</body>
</html>
```

- [ ] **Step 2: 在浏览器打开 index.html，确认页面无报错，显示空白标签选择视图**

用 `python3 -m http.server 8080` 启动本地服务器，访问 `http://localhost:8080`。

- [ ] **Step 3: 提交**

```bash
cd /Users/yangkegang/Desktop/ai-lab/pusaman
git init
git add index.html
git commit -m "feat: add index.html skeleton with two views"
```

---

### Task 2: 创建 style.css

**Files:**
- Create: `style.css`

- [ ] **Step 1: 创建 style.css**

```css
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  background: #f8f9fa;
  color: #222;
  min-height: 100vh;
}

/* 视图切换 */
.view { width: 100%; }
.hidden { display: none !important; }

/* 容器 */
.container {
  margin: 0 auto;
  padding: 40px 20px;
}
.container--labels { max-width: 600px; }
.container--quiz   { max-width: 700px; }

/* 标题 */
.page-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 24px;
}

/* 标签网格 */
.label-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
@media (max-width: 480px) {
  .label-grid { grid-template-columns: repeat(2, 1fr); }
}

.label-card {
  background: #fff;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 12px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}
.label-card:hover { border-color: #aaa; }
.label-card.selected {
  background: #222;
  border-color: #222;
  color: #fff;
}

/* 错误提示 */
.error-msg {
  color: #e53e3e;
  font-size: 14px;
  margin-bottom: 12px;
}

/* 开始按钮区 */
.start-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

/* 按钮 */
.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn:disabled { opacity: 0.4; cursor: not-allowed; }
.btn--primary  { background: #222; color: #fff; }
.btn--primary:hover:not(:disabled)  { background: #444; }
.btn--secondary { background: #f0f0f0; color: #333; }
.btn--secondary:hover:not(:disabled) { background: #e0e0e0; }
.btn-link {
  background: none;
  border: none;
  color: #555;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}
.btn-link:hover { color: #222; }

/* 问答顶部栏 */
.quiz-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.topbar-label {
  font-size: 13px;
  color: #888;
  font-weight: 500;
}

/* 卡片 */
.card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  margin-bottom: 12px;
}

/* Chip */
.chip {
  display: inline-block;
  background: #f0f0f0;
  border-radius: 20px;
  padding: 4px 12px;
  font-size: 12px;
  color: #555;
  margin-bottom: 16px;
}

/* 输入区 */
.answer-input {
  width: 100%;
  min-height: 100px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s;
}
.answer-input:focus { border-color: #aaa; }

/* 按钮行 */
.btn-row {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  margin-bottom: 20px;
}

/* 答案区 */
.answer-area {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.answer-label {
  font-size: 12px;
  color: #888;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
}

/* Markdown 样式 */
.markdown-body { line-height: 1.7; font-size: 15px; }
.markdown-body h1, .markdown-body h2, .markdown-body h3 {
  margin: 16px 0 8px;
  font-weight: 600;
}
.markdown-body p { margin-bottom: 10px; }
.markdown-body ul, .markdown-body ol {
  padding-left: 20px;
  margin-bottom: 10px;
}
.markdown-body li { margin-bottom: 4px; }
.markdown-body code {
  background: #f5f5f5;
  border-radius: 3px;
  padding: 2px 5px;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
}
.markdown-body pre {
  background: #f5f5f5;
  border-radius: 6px;
  padding: 14px 16px;
  overflow-x: auto;
  margin-bottom: 12px;
}
.markdown-body pre code {
  background: none;
  padding: 0;
  font-size: 13px;
}
.markdown-body blockquote {
  border-left: 3px solid #e0e0e0;
  padding-left: 12px;
  color: #666;
  margin-bottom: 10px;
}
.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 12px;
}
.markdown-body th, .markdown-body td {
  border: 1px solid #e0e0e0;
  padding: 6px 12px;
  text-align: left;
}
.markdown-body th { background: #f5f5f5; font-weight: 600; }
```

- [ ] **Step 2: 刷新浏览器，确认页面样式正常，标签网格区域可见，按钮样式正确**

- [ ] **Step 3: 提交**

```bash
git add style.css
git commit -m "feat: add style.css with full layout and markdown styles"
```

---

### Task 3: 创建 app.js — 视图切换与状态管理

**Files:**
- Create: `app.js`

- [ ] **Step 1: 创建 app.js，定义状态和视图切换函数**

```javascript
// 应用状态
const state = {
  selectedLabelId: null,
  selectedLabelName: null,
  currentQuestion: '',
  sseAbortController: null,
};

// DOM 引用
const viewLabels  = document.getElementById('view-labels');
const viewQuiz    = document.getElementById('view-quiz');
const labelGrid   = document.getElementById('label-grid');
const labelError  = document.getElementById('label-error');
const btnRetry    = document.getElementById('btn-retry-labels');
const btnStart    = document.getElementById('btn-start');
const btnBack     = document.getElementById('btn-back');
const quizLabelName = document.getElementById('quiz-label-name');
const quizChip    = document.getElementById('quiz-chip');
const questionCard  = document.getElementById('question-card');
const answerInput   = document.getElementById('answer-input');
const btnSubmit   = document.getElementById('btn-submit');
const btnNext     = document.getElementById('btn-next');
const answerArea  = document.getElementById('answer-area');
const answerContent = document.getElementById('answer-content');

function showView(viewId) {
  viewLabels.classList.toggle('hidden', viewId !== 'labels');
  viewQuiz.classList.toggle('hidden', viewId !== 'quiz');
}

function resetQuizView() {
  answerInput.value = '';
  answerContent.innerHTML = '';
  answerArea.classList.add('hidden');
  questionCard.innerHTML = '';
  btnSubmit.disabled = false;
  btnNext.disabled = false;
}

// 事件：返回
btnBack.addEventListener('click', () => {
  abortSSE();
  showView('labels');
});

// 入口
showView('labels');
loadLabels();
```

- [ ] **Step 2: 刷新浏览器，确认控制台无报错，页面显示标签选择视图**

- [ ] **Step 3: 提交**

```bash
git add app.js
git commit -m "feat: add app.js with state, DOM refs, and view switching"
```

---

### Task 4: 标签列表加载与选择

**Files:**
- Modify: `app.js`

- [ ] **Step 1: 在 app.js 末尾追加标签加载与选择逻辑**

```javascript
async function loadLabels() {
  labelError.classList.add('hidden');
  labelGrid.innerHTML = '';
  try {
    const res = await fetch('/problem/labels');
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const labels = await res.json();
    renderLabels(labels);
  } catch (e) {
    labelError.classList.remove('hidden');
  }
}

function renderLabels(labels) {
  labelGrid.innerHTML = '';
  labels.forEach(({ labelId, labelName }) => {
    const card = document.createElement('div');
    card.className = 'label-card';
    card.textContent = labelName;
    card.dataset.labelId = labelId;
    card.dataset.labelName = labelName;
    card.addEventListener('click', () => selectLabel(card, labelId, labelName));
    labelGrid.appendChild(card);
  });
}

function selectLabel(card, labelId, labelName) {
  // 取消其他选中
  labelGrid.querySelectorAll('.label-card.selected').forEach(c => {
    if (c !== card) c.classList.remove('selected');
  });
  const isSelected = card.classList.toggle('selected');
  state.selectedLabelId   = isSelected ? labelId   : null;
  state.selectedLabelName = isSelected ? labelName : null;
  btnStart.disabled = !isSelected;
}

btnRetry.addEventListener('click', loadLabels);

btnStart.addEventListener('click', () => {
  if (!state.selectedLabelId) return;
  quizLabelName.textContent = state.selectedLabelName;
  quizChip.textContent = state.selectedLabelName;
  resetQuizView();
  showView('quiz');
  loadQuestion();
});
```

- [ ] **Step 2: 刷新浏览器**

由于此时没有后端，在浏览器控制台临时测试 `renderLabels` 是否正常工作：

```javascript
// 在浏览器控制台执行：
renderLabels([{labelId:'1',labelName:'MySQL'},{labelId:'2',labelName:'Kafka'},{labelId:'3',labelName:'Redis'}])
```

确认标签卡片出现、点击可选中高亮、「开始复习」按钮激活。

- [ ] **Step 3: 提交**

```bash
git add app.js
git commit -m "feat: add label loading, rendering, and selection logic"
```

---

### Task 5: 题目加载

**Files:**
- Modify: `app.js`

- [ ] **Step 1: 在 app.js 末尾追加题目加载函数**

```javascript
async function loadQuestion() {
  questionCard.innerHTML = '<span style="color:#aaa;font-size:14px">加载中...</span>';
  btnNext.disabled = true;
  try {
    const res = await fetch(`/problem/${state.selectedLabelId}/next`);
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const questionText = await res.text();
    state.currentQuestion = questionText;
    questionCard.innerHTML = marked.parse(questionText);
    btnNext.disabled = false;
  } catch (e) {
    questionCard.innerHTML = '<span style="color:#e53e3e;font-size:14px">加载失败，请点击「下一个」重试</span>';
    btnNext.disabled = false;
  }
}

btnNext.addEventListener('click', () => {
  abortSSE();
  resetQuizView();
  loadQuestion();
});
```

- [ ] **Step 2: 刷新浏览器，在控制台模拟调用**

```javascript
// 控制台执行（模拟进入问答视图后的状态）：
state.selectedLabelId = '1';
loadQuestion();
// 预期：题目卡片显示"加载中..."后显示错误（因为没有后端），但不崩溃
```

- [ ] **Step 3: 提交**

```bash
git add app.js
git commit -m "feat: add question loading with markdown rendering and error state"
```

---

### Task 6: SSE 流式答案

**Files:**
- Modify: `app.js`

- [ ] **Step 1: 在 app.js 末尾追加 SSE 处理与提交逻辑**

```javascript
function abortSSE() {
  if (state.sseAbortController) {
    state.sseAbortController.abort();
    state.sseAbortController = null;
  }
}

btnSubmit.addEventListener('click', async () => {
  const userAnswer = answerInput.value.trim();
  if (!userAnswer) return;

  abortSSE();
  state.sseAbortController = new AbortController();

  btnSubmit.disabled = true;
  btnNext.disabled = true;
  answerContent.innerHTML = '';
  answerArea.classList.remove('hidden');

  let accumulated = '';

  try {
    const res = await fetch('/problem/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: state.currentQuestion, answer: userAnswer }),
      signal: state.sseAbortController.signal,
    });

    if (!res.ok) throw new Error('HTTP ' + res.status);

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 保留未完成的行

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const chunk = line.slice(5).trimStart();
          if (chunk === '[DONE]') break;
          accumulated += chunk;
          answerContent.innerHTML = marked.parse(accumulated);
        }
      }
    }

    // 处理最后一行
    if (buffer.startsWith('data:')) {
      const chunk = buffer.slice(5).trimStart();
      if (chunk && chunk !== '[DONE]') {
        accumulated += chunk;
        answerContent.innerHTML = marked.parse(accumulated);
      }
    }

  } catch (e) {
    if (e.name === 'AbortError') return; // 用户主动中止，不报错
    answerContent.innerHTML = marked.parse(accumulated) +
      '<p style="color:#e53e3e;font-size:13px;margin-top:8px">（连接中断，请重试）</p>';
  } finally {
    btnSubmit.disabled = false;
    btnNext.disabled = false;
    state.sseAbortController = null;
  }
});
```

- [ ] **Step 2: 刷新浏览器，在控制台验证 abortSSE 函数可调用无报错**

```javascript
abortSSE(); // 预期：无报错，静默完成
```

- [ ] **Step 3: 提交**

```bash
git add app.js
git commit -m "feat: add SSE streaming answer with abort support and error handling"
```

---

### Task 7: 端到端验证

**Files:** 无新文件

- [ ] **Step 1: 启动本地服务器**

```bash
python3 -m http.server 8080
```

访问 `http://localhost:8080`

- [ ] **Step 2: 验证标签选择视图**

- 页面加载后显示"选择复习标签"标题
- 若后端未启动，显示"加载失败，重试"提示
- 有后端时：标签网格正确渲染，点击标签高亮，「开始复习」激活

- [ ] **Step 3: 验证问答视图**

- 进入问答后题目卡片 Markdown 渲染正确
- 标签 chip 显示正确
- 输入答案后点「提交」，答案区展开并流式显示内容
- 点「下一个」清空答案区，加载新题目
- 点「返回」回到标签选择视图

- [ ] **Step 4: 验证 SSE 中断**

- 提交后立即点「下一个」或「返回」，确认无控制台报错（AbortError 被捕获）

- [ ] **Step 5: 最终提交**

```bash
git add .
git commit -m "chore: final integration verified"
```

---

## Self-Review

**Spec 覆盖检查：**

| 需求 | 对应 Task |
|------|-----------|
| 展示标签，从 `/problem/labels` 获取 | Task 4 |
| 用户选择标签后进入问答 | Task 4 `btnStart` |
| 展示题目（Markdown）和标签 chip | Task 5 + Task 2 |
| 「下一个」按钮加载新题 | Task 5 `btnNext` |
| 输入框 + 「提交」调用 `/problem/ask` SSE | Task 6 |
| SSE 流式渲染，Markdown 格式化 | Task 6 |
| 错误处理：标签/题目加载失败 | Task 4、Task 5 |
| SSE 中断提示 | Task 6 |

**无遗漏，无占位符，类型一致。**
