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
  // 不清空题目卡片，保留当前题目直到新题目加载完成，避免页面抖动
  btnSubmit.disabled = true;
  btnNext.disabled = false;
}

function abortSSE() {
  if (state.sseAbortController) {
    state.sseAbortController.abort();
    state.sseAbortController = null;
  }
}

function extractJsonValues(raw) {
  let result = '';
  // 已完整闭合的 value
  const re = /"(?:j|r)"\s*:\s*"((?:[^"\\]|\\.)*)"/g;
  let m;
  let lastIndex = 0;
  while ((m = re.exec(raw)) !== null) {
    result += m[1];
    lastIndex = re.lastIndex;
  }
  // 最后一个未闭合的 value（还在流式输出中）
  const partial = raw.slice(lastIndex).match(/"(?:j|r)"\s*:\s*"((?:[^"\\]|\\.)*)/);
  if (partial) result += partial[1];
  return result.replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\"/g, '"');
}

function renderAnswer(judgement, referAnswer) {
  answerContent.innerHTML =
    '<div class="answer-section">' +
      '<div class="answer-section-title"><span class="answer-icon answer-icon--judge">💬</span>系统评判</div>' +
      '<div class="answer-section-body markdown-body">' + marked.parse(judgement || '') + '</div>' +
    '</div>' +
    '<div class="answer-section">' +
      '<div class="answer-section-title"><span class="answer-icon answer-icon--answer">📖</span>参考答案</div>' +
      '<div class="answer-section-body markdown-body">' + marked.parse(referAnswer || '') + '</div>' +
    '</div>';
}

// 事件：返回
btnBack.addEventListener('click', () => {
  abortSSE();
  showView('labels');
});

// Task 4: 标签加载
async function loadLabels() {
  labelError.classList.add('hidden');
  labelGrid.innerHTML = '<span style="color:#aaa;font-size:14px">加载中...</span>';
  try {
    const res = await fetch(`/problem/labels`);
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const labels = await res.json();
    renderLabels(labels);
  } catch (e) {
    labelError.classList.remove('hidden');
  }
}

function renderLabels(labels) {
  labelGrid.innerHTML = '';
  labels.forEach(({ labelId, labelName, problemCnt }) => {
    const card = document.createElement('div');
    card.className = 'label-card';
    card.textContent = labelName + '（' + problemCnt + '）';
    card.dataset.labelId = labelId;
    card.dataset.labelName = labelName;
    card.addEventListener('click', () => selectLabel(card, labelId, labelName));
    card.setAttribute('tabindex', '0');
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        selectLabel(card, labelId, labelName);
      }
    });
    labelGrid.appendChild(card);
  });
}

function selectLabel(card, labelId, labelName) {
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
  quizChip.textContent = state.selectedLabelName;
  resetQuizView();
  showView('quiz');
  loadQuestion();
});

// Task 5: 题目加载
async function loadQuestion() {
  // 不提前清空题目，保留旧题目直到新题目加载完成，避免页面抖动
  btnNext.disabled = true;
  btnSubmit.disabled = true;
  try {
    const res = await fetch(`/problem/${state.selectedLabelId}/next`);
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const questionText = await res.text();
    state.currentQuestion = questionText;
    questionCard.innerHTML = marked.parse(questionText);
    btnNext.disabled = false;
    btnSubmit.disabled = answerInput.value.trim() === '';
  } catch (e) {
    questionCard.innerHTML = '<span style="color:#e53e3e;font-size:14px">加载失败，请点击「下一个」重试</span>';
    btnNext.disabled = false;
    btnSubmit.disabled = answerInput.value.trim() === '';
  }
}

btnNext.addEventListener('click', () => {
  abortSSE();
  resetQuizView();
  loadQuestion();
});

// Task 6: SSE 流式答案
btnSubmit.addEventListener('click', async () => {
  const userAnswer = answerInput.value.trim();
  if (!userAnswer) return;

  abortSSE();
  state.sseAbortController = new AbortController();

  btnSubmit.disabled = true;
  btnNext.disabled = true;
  answerContent.innerHTML = '<div class="thinking"><div class="thinking-dots"><span></span><span></span><span></span></div>Thinking...</div>';
  answerArea.classList.remove('hidden');

  let aborted = false;

  try {
    const params = new URLSearchParams({ q: state.currentQuestion, a: userAnswer });
    const res = await fetch(`/problem/ask?${params}`, {
      method: 'GET',
      signal: state.sseAbortController.signal,
    });

    if (!res.ok) throw new Error('HTTP ' + res.status);

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let accumulated = '';

    outer: while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop();

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const chunk = line.slice(5);
          if (chunk === '[DONE]') break outer;
          accumulated += chunk;
          answerContent.innerHTML =
            '<pre class="typewriter">' + extractJsonValues(accumulated).replace(/</g, '&lt;') + '</pre>';
        }
      }
    }

    if (buffer.startsWith('data:')) {
      const chunk = buffer.slice(5);
      if (chunk && chunk !== '[DONE]') accumulated += chunk;
    }

    try {
      const data = JSON.parse(accumulated);
      renderAnswer(data.j, data.r);
    } catch (_) {
      answerContent.innerHTML = '<p style="color:#e53e3e;font-size:13px;">（解析失败，请重试）</p>';
    }

  } catch (e) {
    if (e.name === 'AbortError') {
      aborted = true;
    } else {
      answerContent.innerHTML = '<p style="color:#e53e3e;font-size:13px;">（连接中断，请重试）</p>';
    }
  } finally {
    if (!aborted) {
      btnSubmit.disabled = answerInput.value.trim() === '';
      btnNext.disabled = false;
    }
    state.sseAbortController = null;
  }
});

// 输入框为空时禁用提交按钮
answerInput.addEventListener('input', () => {
  btnSubmit.disabled = answerInput.value.trim() === '';
});

// 入口
showView('labels');
loadLabels();
