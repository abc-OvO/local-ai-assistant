const state = {
  documents: [],
  selectedDocumentId: null,
  mode: "auto",
  sessionId: "default",
  sending: false
};

const els = {
  toastHost: document.querySelector("#toastHost"),
  generationProviderText: document.querySelector("#generationProviderText"),
  embeddingProviderText: document.querySelector("#embeddingProviderText"),
  embeddingModelText: document.querySelector("#embeddingModelText"),
  documentCountText: document.querySelector("#documentCountText"),
  chunkTotalText: document.querySelector("#chunkTotalText"),
  workspaceStatusText: document.querySelector("#workspaceStatusText"),
  providerSelect: document.querySelector("#providerSelect"),
  applyProviderBtn: document.querySelector("#applyProviderBtn"),
  providerStatus: document.querySelector("#providerStatus"),
  refreshAllBtn: document.querySelector("#refreshAllBtn"),
  fileInput: document.querySelector("#fileInput"),
  fileNameText: document.querySelector("#fileNameText"),
  uploadBtn: document.querySelector("#uploadBtn"),
  uploadStatus: document.querySelector("#uploadStatus"),
  refreshDocumentsBtn: document.querySelector("#refreshDocumentsBtn"),
  documentStatus: document.querySelector("#documentStatus"),
  documentList: document.querySelector("#documentList"),
  sessionIdInput: document.querySelector("#sessionIdInput"),
  newSessionBtn: document.querySelector("#newSessionBtn"),
  clearMemoryBtn: document.querySelector("#clearMemoryBtn"),
  sessionStatus: document.querySelector("#sessionStatus"),
  sessionList: document.querySelector("#sessionList"),
  modeMenuTrigger: document.querySelector("#modeMenuTrigger"),
  modeMenu: document.querySelector("#modeMenu"),
  currentModeLabel: document.querySelector("#currentModeLabel"),
  autoModeBtn: document.querySelector("#autoModeBtn"),
  globalModeBtn: document.querySelector("#globalModeBtn"),
  singleModeBtn: document.querySelector("#singleModeBtn"),
  chatModeBtn: document.querySelector("#chatModeBtn"),
  activeContextText: document.querySelector("#activeContextText"),
  messageList: document.querySelector("#messageList"),
  chunksList: document.querySelector("#chunksList"),
  questionInput: document.querySelector("#questionInput"),
  askBtn: document.querySelector("#askBtn"),
  askStatus: document.querySelector("#askStatus")
};

const modeButtons = {
  auto: els.autoModeBtn,
  chat: els.chatModeBtn,
  single: els.singleModeBtn,
  global: els.globalModeBtn,
};

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message = typeof payload === "object" && payload !== null
      ? payload.msg || JSON.stringify(payload)
      : payload || response.statusText;
    throw new Error(message);
  }

  if (payload && typeof payload === "object" && "code" in payload) {
    if (payload.code !== 0) {
      throw new Error(payload.msg || "请求失败");
    }
    return payload.data;
  }

  return payload;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function formatScore(score) {
  const value = Number(score);
  return Number.isFinite(value) ? value.toFixed(4) : "-";
}

function setStatus(el, message, type = "") {
  el.textContent = message;
  el.className = `ws-status ${type}`.trim();
}

function setBusy(button, busy, text) {
  button.disabled = busy;
  if (text) {
    button.textContent = text;
  }
}

function setWorkspaceStatus(status, type = "") {
  if (!els.workspaceStatusText) {
    return;
  }
  els.workspaceStatusText.textContent = status;
  els.workspaceStatusText.className = type;
}

function toast(message, type = "success") {
  const item = document.createElement("div");
  item.className = `toast ${type}`;
  item.textContent = message;
  els.toastHost.appendChild(item);
  setTimeout(() => item.remove(), 3200);
}

function selectedDocument() {
  return state.documents.find((doc) => doc.documentId === state.selectedDocumentId) || null;
}

function currentSessionId() {
  const sessionId = els.sessionIdInput.value.trim() || "default";
  state.sessionId = sessionId;
  els.sessionStatus.textContent = `当前 session：${sessionId}`;
  return sessionId;
}

async function loadProvider() {
  setStatus(els.providerStatus, "正在读取模型状态...", "loading");
  try {
    const status = await requestJson("/api/settings/model");
    const models = Array.isArray(status.availableModels) ? status.availableModels : [];
    els.providerSelect.innerHTML = models.map((model) => `
      <option value="${escapeHtml(model)}">${escapeHtml(formatModelName(model))}</option>
    `).join("");
    els.providerSelect.value = status.currentModel || models[0] || "";
    if (els.generationProviderText) {
      els.generationProviderText.textContent = status.currentModel || "-";
    }
    els.embeddingModelText.textContent = status.embeddingModel || "-";
    setStatus(els.providerStatus, `当前生成模型：${formatModelName(status.currentModel || "-")}`, "success");
  } catch (error) {
    setStatus(els.providerStatus, `读取失败：${error.message}`, "error");
  }
}

async function switchProvider() {
  const model = els.providerSelect.value;
  setBusy(els.applyProviderBtn, true, "应用中");
  try {
    await requestJson("/api/settings/model", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ model })
    });
    await loadProvider();
    toast(`已切换生成模型：${formatModelName(model)}`);
  } catch (error) {
    setStatus(els.providerStatus, `切换失败：${error.message}`, "error");
    toast(`切换失败：${error.message}`, "error");
  } finally {
    setBusy(els.applyProviderBtn, false, "应用");
  }
}

function formatModelName(model) {
  return String(model || "")
    .replace(/^kimi-/i, "Kimi ")
    .replace(/\bk(\d)/i, "K$1")
    .replace(/-thinking$/i, " Thinking")
    .replace(/-/g, " ");
}

async function loadDocuments() {
  setStatus(els.documentStatus, "正在同步文档...", "loading");
  try {
    const docs = await requestJson("/api/documents");
    state.documents = Array.isArray(docs) ? docs : [];
    if (state.selectedDocumentId && !state.documents.some((doc) => doc.documentId === state.selectedDocumentId)) {
      state.selectedDocumentId = null;
    }
    renderDocuments();
    setStatus(els.documentStatus, `已同步 ${state.documents.length} 个文档。`, "success");
  } catch (error) {
    setStatus(els.documentStatus, `同步失败：${error.message}`, "error");
  }
}

function renderDocuments() {
  const chunkTotal = state.documents.reduce((sum, doc) => sum + Number(doc.chunkCount || 0), 0);
  if (els.documentCountText) {
    els.documentCountText.textContent = String(state.documents.length);
  }
  if (els.chunkTotalText) {
    els.chunkTotalText.textContent = String(chunkTotal);
  }
  els.documentList.innerHTML = "";

  if (state.documents.length === 0) {
    els.documentList.innerHTML = '<div class="ws-empty">暂无文档，请先上传 txt、md 或 pdf 文件。</div>';
    updateModeText();
    return;
  }

  for (const doc of state.documents) {
    const item = document.createElement("article");
    item.className = doc.documentId === state.selectedDocumentId
      ? "document-item selected"
      : "document-item";
    item.innerHTML = `
      <button class="document-main" type="button">
        <span class="document-title">${escapeHtml(doc.fileName)}</span>
        <span class="document-meta">✓ ${escapeHtml(doc.fileType || "-")} · chunks ${escapeHtml(doc.chunkCount ?? 0)} · ${escapeHtml(formatDateTime(doc.createdAt || doc.uploadTime))}</span>
      </button>
      <div class="document-actions">
        <button class="ws-secondary select-btn" type="button">选择</button>
        <button class="ws-danger delete-btn" type="button">删除</button>
      </div>
    `;
    item.querySelector(".document-main").addEventListener("click", () => selectDocument(doc.documentId));
    item.querySelector(".select-btn").addEventListener("click", () => selectDocument(doc.documentId));
    item.querySelector(".delete-btn").addEventListener("click", () => deleteDocument(doc.documentId, doc.fileName));
    els.documentList.appendChild(item);
  }

  updateModeText();
}

function selectDocument(documentId) {
  state.selectedDocumentId = documentId;
  renderDocuments();
  updateModeText();
}

async function deleteDocument(documentId, fileName) {
  if (!window.confirm(`确认删除文档“${fileName}”吗？`)) {
    return;
  }
  try {
    await requestJson(`/api/documents/${encodeURIComponent(documentId)}`, { method: "DELETE" });
    if (state.selectedDocumentId === documentId) {
      state.selectedDocumentId = null;
    }
    await loadDocuments();
    toast("文档已删除");
  } catch (error) {
    toast(`删除失败：${error.message}`, "error");
  }
}

async function uploadDocument() {
  const file = els.fileInput.files[0];
  if (!file) {
    setStatus(els.uploadStatus, "请先选择一个文件。", "error");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);
  setBusy(els.uploadBtn, true, "索引中");
  setWorkspaceStatus("索引中...", "loading");
  setStatus(els.uploadStatus, "正在上传并生成向量索引...", "loading");

  try {
    const result = await requestJson("/api/documents/upload", {
      method: "POST",
      body: formData
    });
    state.selectedDocumentId = result.documentId;
    els.fileInput.value = "";
    els.fileNameText.textContent = "选择知识库文件";
    await loadDocuments();
    setStatus(els.uploadStatus, `已上传 ${file.name}，并加入知识库。`, "success");
    toast(`已上传：${file.name}`);
  } catch (error) {
    setStatus(els.uploadStatus, `上传失败：${error.message}`, "error");
    toast(`上传失败：${error.message}`, "error");
  } finally {
    setWorkspaceStatus("就绪", "success");
    setBusy(els.uploadBtn, false, "上传并索引");
  }
}

async function loadSessions() {
  try {
    const sessions = await requestJson("/api/chat/sessions");
    renderSessions(Array.isArray(sessions) ? sessions : []);
  } catch (error) {
    els.sessionList.innerHTML = '<div class="ws-empty">会话列表读取失败。</div>';
  }
}

function renderSessions(sessions) {
  if (sessions.length === 0) {
    els.sessionList.innerHTML = '<div class="ws-empty">暂无历史会话。</div>';
    return;
  }

  els.sessionList.innerHTML = sessions.map((session) => `
    <button class="session-item" type="button" data-session="${escapeHtml(session.sessionId)}">
      <span class="session-title">${escapeHtml(session.sessionId)}</span>
      <span class="session-meta">${escapeHtml(session.messageCount ?? 0)} 条消息 · ${escapeHtml(formatDateTime(session.lastMessageAt))}</span>
    </button>
  `).join("");

  els.sessionList.querySelectorAll(".session-item").forEach((item) => {
    item.addEventListener("click", () => {
      els.sessionIdInput.value = item.dataset.session;
      currentSessionId();
      toast("已切换会话");
    });
  });
}

function newSession() {
  const id = `session-${Date.now().toString(36)}`;
  els.sessionIdInput.value = id;
  currentSessionId();
  clearMessages();
  toast("已开始新会话");
}

async function clearMemory() {
  const sessionId = currentSessionId();
  try {
    await requestJson(`/api/chat/memory/${encodeURIComponent(sessionId)}`, { method: "DELETE" });
    await loadSessions();
    toast("当前会话上下文已清除");
  } catch (error) {
    toast(`清空失败：${error.message}`, "error");
  }
}

function updateMode(mode) {
  state.mode = mode;
  for (const [name, button] of Object.entries(modeButtons)) {
    const active = name === mode;
    button.classList.toggle("active", active);
    button.setAttribute("aria-checked", String(active));
  }
  els.currentModeLabel.textContent = {
    auto: "自动模式",
    chat: "普通对话",
    single: "单文档问答",
    global: "全局知识库"
  }[mode];
  closeModeMenu();
  updateModeText();
}

function openModeMenu() {
  els.modeMenu.hidden = false;
  els.modeMenuTrigger.setAttribute("aria-expanded", "true");
}

function closeModeMenu() {
  els.modeMenu.hidden = true;
  els.modeMenuTrigger.setAttribute("aria-expanded", "false");
}

function toggleModeMenu() {
  if (els.modeMenu.hidden) {
    openModeMenu();
  } else {
    closeModeMenu();
  }
}

function updateModeText() {
  const doc = selectedDocument();
  if (state.mode === "single") {
    els.activeContextText.textContent = doc
      ? `单文档 RAG：${doc.fileName}`
      : "使用单文档 RAG 前，请先选择一个文档。";
  } else if (state.mode === "global") {
    els.activeContextText.textContent = "全局知识库会检索全部已索引文档。";
  } else if (state.mode === "auto") {
    els.activeContextText.textContent = "自动判断直接对话或知识库检索。";
  } else {
    els.activeContextText.textContent = "普通对话只调用生成模型，不检索知识库。";
  }
}

async function buildAskRequest(question) {
  if (state.mode === "single") {
    return {
      url: "/api/knowledge/ask",
      body: { documentId: state.selectedDocumentId, question },
      metadata: { ragUsed: true }
    };
  }

  if (state.mode === "global") {
    return {
      url: "/api/knowledge/ask-global",
      body: { sessionId: currentSessionId(), question },
      metadata: { ragUsed: true }
    };
  }

  if (state.mode === "auto") {
    const plannerStart = performance.now();
    const planner = await requestJson("/api/planner", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question })
    });
    const intent = planner.intent === "DIRECT_CHAT" ? "DIRECT_CHAT" : "KNOWLEDGE_SEARCH";
    const plannerCostMs = Math.round(performance.now() - plannerStart);
    const route = WorkspaceAuto.buildAutoRoute(intent, currentSessionId(), question);
    route.metadata.plannerCostMs = plannerCostMs;
    return route;
  }

  return {
    url: "/api/chat",
    body: { sessionId: currentSessionId(), message: question },
    metadata: { ragUsed: false }
  };
}

async function ask() {
  const question = els.questionInput.value.trim();
  if (!question || state.sending) {
    return;
  }

  if (state.mode === "single" && !state.selectedDocumentId) {
    toast("请先选择一个文档", "error");
    return;
  }

  state.sending = true;
  setBusy(els.askBtn, true, "生成中");
  setWorkspaceStatus(state.mode === "auto" ? "规划中..." : "思考中...", "loading");
  setStatus(els.askStatus, state.mode === "auto" ? "正在判断是否需要检索..." : "正在生成回答...", "loading");
  appendMessage("user", question);
  const assistantMessage = appendAssistantLoading();
  els.questionInput.value = "";
  renderChunks([]);

  try {
    const request = await buildAskRequest(question);
    setWorkspaceStatus(request.metadata?.ragUsed ? "检索中..." : "思考中...", "loading");
    setStatus(els.askStatus, request.metadata?.ragUsed ? "正在检索知识库并生成回答..." : "正在生成回答...", "loading");
    const result = await requestJson(request.url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request.body)
    });
    const chunks = WorkspaceAuto.chunksForAssistant(request.metadata, result);
    await renderAssistantResult(
      assistantMessage,
      result.reply || "",
      result.model,
      chunks,
      request.metadata
    );
    await loadSessions();
    setStatus(els.askStatus, "回答完成。", "success");
  } catch (error) {
    const friendly = error.message.includes("\u5f53\u524d\u6ca1\u6709\u4efb\u4f55\u53ef\u68c0\u7d22\u6587\u6863")
      ? "请先上传文档，再使用全局知识库。"
      : error.message;
    renderAssistantError(assistantMessage, friendly);
    setStatus(els.askStatus, `请求失败：${friendly}`, "error");
  } finally {
    state.sending = false;
    setWorkspaceStatus("就绪", "success");
    setBusy(els.askBtn, false, "发送");
  }
}

function clearMessages() {
  els.messageList.innerHTML = `
    <div class="ws-welcome">
      <p class="ws-label">新会话</p>
      <h3>新的会话已准备好。</h3>
      <p>上传文档或直接提问，Local AI Workspace 会在这里保留上下文。</p>
    </div>
  `;
}

function appendMessage(role, content, model = "", isError = false) {
  if (els.messageList.querySelector(".ws-empty, .ws-welcome")) {
    els.messageList.innerHTML = "";
  }
  const item = document.createElement("article");
  item.className = `message ${role} ${isError ? "error" : ""}`.trim();
  item.innerHTML = `
    <div class="message-role">${role === "user" ? "用户" : "AI"}${model ? `<span>${escapeHtml(model)}</span>` : ""}</div>
    <p>${escapeHtml(content)}</p>
  `;
  els.messageList.appendChild(item);
  els.messageList.scrollTop = els.messageList.scrollHeight;
  return item;
}

function appendAssistantLoading() {
  if (els.messageList.querySelector(".ws-empty, .ws-welcome")) {
    els.messageList.innerHTML = "";
  }

  const item = document.createElement("article");
  item.className = "message assistant loading";
  item.innerHTML = `
    <div class="message-role">AI</div>
    <div class="typing-dots" aria-label="AI 正在思考">
      <span></span><span></span><span></span>
    </div>
    <div class="planner-badge" hidden></div>
    <p class="message-content"></p>
    <div class="message-sources"></div>
  `;
  els.messageList.appendChild(item);
  els.messageList.scrollTop = els.messageList.scrollHeight;
  return item;
}

async function renderAssistantResult(item, content, model = "", chunks = [], metadata = {}) {
  item.className = "message assistant";
  item.querySelector(".message-role").innerHTML = `AI${model ? `<span>${escapeHtml(model)}</span>` : ""}`;
  const dots = item.querySelector(".typing-dots");
  if (dots) {
    dots.remove();
  }

  renderPlannerBadge(item.querySelector(".planner-badge"), metadata);
  const contentEl = item.querySelector(".message-content");
  await streamText(contentEl, content || "未返回回答。");
  renderSources(item.querySelector(".message-sources"), metadata.ragUsed === false ? [] : chunks);
  els.messageList.scrollTop = els.messageList.scrollHeight;
}

function renderPlannerBadge(target, metadata = {}) {
  if (!target || metadata.mode !== "auto") {
    if (target) {
      target.hidden = true;
      target.innerHTML = "";
    }
    return;
  }

  target.hidden = false;
  target.innerHTML = `
    <span>Auto Planner</span>
    <strong>${escapeHtml(metadata.intent || "-")}</strong>
    <em>${metadata.ragUsed ? "RAG" : "Chat"} · ${escapeHtml(metadata.plannerCostMs ?? "-")}ms</em>
  `;
}

function renderAssistantError(item, message) {
  item.className = "message assistant error";
  item.querySelector(".message-role").textContent = "AI";
  const dots = item.querySelector(".typing-dots");
  if (dots) {
    dots.remove();
  }
  item.querySelector(".message-content").textContent = message;
  item.querySelector(".message-sources").innerHTML = "";
  els.messageList.scrollTop = els.messageList.scrollHeight;
}

function streamText(target, content) {
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches || content.length > 1200) {
    target.textContent = content;
    return Promise.resolve();
  }

  target.textContent = "";
  return new Promise((resolve) => {
    let index = 0;
    const step = () => {
      target.textContent = content.slice(0, index);
      index += 2;
      els.messageList.scrollTop = els.messageList.scrollHeight;
      if (index <= content.length + 2) {
        window.setTimeout(step, 14);
      } else {
        target.textContent = content;
        resolve();
      }
    };
    step();
  });
}

function renderScoreLine(chunk) {
  if (chunk.finalScore == null && chunk.vectorScore == null && chunk.keywordScore == null) {
    return `<span>score ${formatScore(chunk.score)}</span>`;
  }
  return `
    <span>final ${formatScore(chunk.finalScore ?? chunk.score)}</span>
    <span>vector ${formatScore(chunk.vectorScore)}</span>
    <span>keyword ${formatScore(chunk.keywordScore)}</span>
  `;
}

function chunkSourceTitle(chunk) {
  const fileName = chunk.fileName || chunk.documentName || "未知文档";
  const index = chunk.chunkIndex ?? chunk.index ?? "-";
  return `${fileName} · Chunk ${index}`;
}

function renderSources(container, chunks) {
  if (!chunks || chunks.length === 0) {
    container.innerHTML = "";
    return;
  }

  container.innerHTML = `
    <div class="sources-compact">
      <p class="sources-title">来源</p>
      ${chunks.slice(0, 4).map((chunk) => `
        <div class="source-row">
          <span>${escapeHtml(chunk.fileName || chunk.documentName || "未知文档")}</span>
          <strong>Chunk ${escapeHtml(chunk.chunkIndex ?? chunk.index ?? "-")}</strong>
        </div>
      `).join("")}
    </div>
    <details class="sources-detail">
      <summary>查看检索依据</summary>
      <div class="source-detail-list">
        ${chunks.map((chunk) => `
          <article class="source-detail-card">
            <div>
              <strong>${escapeHtml(chunkSourceTitle(chunk))}</strong>
              <span>final ${formatScore(chunk.finalScore ?? chunk.score)} · vector ${formatScore(chunk.vectorScore)} · keyword ${formatScore(chunk.keywordScore)}</span>
            </div>
            <p>${escapeHtml(chunk.contentPreview || "")}</p>
          </article>
        `).join("")}
      </div>
    </details>
  `;
}

function renderChunks(chunks) {
  els.chunksList.innerHTML = "";
  els.chunksList.hidden = true;
}

async function refreshAll() {
  await Promise.all([loadProvider(), loadDocuments(), loadSessions()]);
}

els.applyProviderBtn.addEventListener("click", switchProvider);
els.refreshAllBtn.addEventListener("click", refreshAll);
els.refreshDocumentsBtn.addEventListener("click", loadDocuments);
els.uploadBtn.addEventListener("click", uploadDocument);
els.fileInput.addEventListener("change", () => {
  const file = els.fileInput.files[0];
  els.fileNameText.textContent = file ? file.name : "选择知识库文件";
});
els.sessionIdInput.addEventListener("input", currentSessionId);
els.newSessionBtn.addEventListener("click", newSession);
els.clearMemoryBtn.addEventListener("click", clearMemory);
els.modeMenuTrigger.addEventListener("click", toggleModeMenu);
els.autoModeBtn.addEventListener("click", () => updateMode("auto"));
els.chatModeBtn.addEventListener("click", () => updateMode("chat"));
els.singleModeBtn.addEventListener("click", () => updateMode("single"));
els.globalModeBtn.addEventListener("click", () => updateMode("global"));
els.askBtn.addEventListener("click", ask);
els.questionInput.addEventListener("keydown", (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
    ask();
  }
});
document.addEventListener("click", (event) => {
  if (!event.target.closest(".ws-mode-picker")) {
    closeModeMenu();
  }
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeModeMenu();
    els.modeMenuTrigger.focus();
  }
});

currentSessionId();
updateMode("auto");
refreshAll();
