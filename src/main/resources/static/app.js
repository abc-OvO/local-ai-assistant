const state = {
  documents: [],
  selectedDocumentId: null,
  mode: "global",
  sessionId: "default",
  provider: null,
  sending: false
};

const els = {
  toastHost: document.querySelector("#toastHost"),
  generationProviderText: document.querySelector("#generationProviderText"),
  embeddingProviderText: document.querySelector("#embeddingProviderText"),
  embeddingModelText: document.querySelector("#embeddingModelText"),
  documentCountText: document.querySelector("#documentCountText"),
  chunkTotalText: document.querySelector("#chunkTotalText"),
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
  detailTitle: document.querySelector("#detailTitle"),
  detailMeta: document.querySelector("#detailMeta"),
  documentDetail: document.querySelector("#documentDetail"),
  sessionIdInput: document.querySelector("#sessionIdInput"),
  newSessionBtn: document.querySelector("#newSessionBtn"),
  clearMemoryBtn: document.querySelector("#clearMemoryBtn"),
  sessionStatus: document.querySelector("#sessionStatus"),
  sessionList: document.querySelector("#sessionList"),
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
  global: els.globalModeBtn,
  single: els.singleModeBtn,
  chat: els.chatModeBtn
};

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json") ? await response.json() : await response.text();

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

function toast(message, type = "success") {
  const item = document.createElement("div");
  item.className = `toast ${type}`;
  item.textContent = message;
  els.toastHost.appendChild(item);
  setTimeout(() => item.remove(), 3200);
}

function setStatus(el, message, type = "") {
  el.textContent = message;
  el.className = `status ${type}`.trim();
}

function setBusy(button, busy, text) {
  button.disabled = busy;
  if (text) button.textContent = text;
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
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function formatScore(score) {
  const value = Number(score);
  return Number.isFinite(value) ? value.toFixed(4) : "-";
}

function currentSessionId() {
  const sessionId = els.sessionIdInput.value.trim() || "default";
  state.sessionId = sessionId;
  els.sessionStatus.textContent = `当前 session：${sessionId}`;
  return sessionId;
}

function selectedDocument() {
  return state.documents.find((doc) => doc.documentId === state.selectedDocumentId) || null;
}

async function loadProvider() {
  try {
    const status = await requestJson("/api/settings/provider");
    state.provider = status.currentProvider;
    els.providerSelect.value = status.currentProvider;
    els.generationProviderText.textContent = status.generationProvider;
    els.embeddingProviderText.textContent = status.embeddingProvider;
    els.embeddingModelText.textContent = status.embeddingModel;
    setStatus(els.providerStatus, `generation=${status.generationProvider}, embedding=${status.embeddingProvider}`, "success");
  } catch (error) {
    setStatus(els.providerStatus, `Provider 状态读取失败：${error.message}`, "error");
    toast("Provider 状态读取失败", "error");
  }
}

async function switchProvider() {
  const provider = els.providerSelect.value;
  setBusy(els.applyProviderBtn, true, "应用中");
  try {
    await requestJson("/api/settings/provider", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ provider })
    });
    await loadProvider();
    toast(`已切换 generation provider：${provider}`);
  } catch (error) {
    setStatus(els.providerStatus, `切换失败：${error.message}`, "error");
    toast(`切换失败：${error.message}`, "error");
  } finally {
    setBusy(els.applyProviderBtn, false, "应用");
  }
}

async function loadDocuments() {
  setStatus(els.documentStatus, "正在同步文档...", "loading");
  try {
    const docs = await requestJson("/api/documents");
    state.documents = Array.isArray(docs) ? docs : [];
    if (state.selectedDocumentId && !state.documents.some((doc) => doc.documentId === state.selectedDocumentId)) {
      state.selectedDocumentId = null;
      clearDocumentDetail();
    }
    renderDocuments();
    setStatus(els.documentStatus, `已同步 ${state.documents.length} 个文档。`, "success");
  } catch (error) {
    setStatus(els.documentStatus, `同步失败：${error.message}`, "error");
    toast("文档同步失败", "error");
  }
}

function renderDocuments() {
  const chunkTotal = state.documents.reduce((sum, doc) => sum + Number(doc.chunkCount || 0), 0);
  els.documentCountText.textContent = String(state.documents.length);
  els.chunkTotalText.textContent = String(chunkTotal);
  els.documentList.innerHTML = "";

  if (state.documents.length === 0) {
    els.documentList.innerHTML = '<div class="empty-state">暂无文档，请先上传。</div>';
    return;
  }

  for (const doc of state.documents) {
    const item = document.createElement("article");
    item.className = doc.documentId === state.selectedDocumentId ? "document-item selected" : "document-item";
    item.innerHTML = `
      <button class="document-main" type="button">
        <strong>${escapeHtml(doc.fileName)}</strong>
        <span>${escapeHtml(doc.fileType || "-")} · chunks ${escapeHtml(doc.chunkCount ?? 0)} · ${escapeHtml(formatDateTime(doc.createdAt || doc.uploadTime))}</span>
      </button>
      <div class="document-actions">
        <button class="ghost-button detail-btn" type="button">详情</button>
        <button class="ghost-button danger delete-btn" type="button">删除</button>
      </div>
    `;
    item.querySelector(".document-main").addEventListener("click", () => selectDocument(doc.documentId));
    item.querySelector(".detail-btn").addEventListener("click", () => loadDocumentDetail(doc.documentId));
    item.querySelector(".delete-btn").addEventListener("click", () => deleteDocument(doc.documentId, doc.fileName));
    els.documentList.appendChild(item);
  }
}

function selectDocument(documentId) {
  state.selectedDocumentId = documentId;
  renderDocuments();
  updateModeText();
}

async function loadDocumentDetail(documentId) {
  try {
    const detail = await requestJson(`/api/documents/${encodeURIComponent(documentId)}`);
    state.selectedDocumentId = documentId;
    renderDocuments();
    els.detailTitle.textContent = detail.fileName || documentId;
    els.detailMeta.textContent = `${detail.fileType || "-"} · chunks ${detail.chunkCount ?? 0} · updated ${formatDateTime(detail.updatedAt)}`;
    els.documentDetail.className = "detail-box";
    els.documentDetail.innerHTML = `
      <p>${escapeHtml(detail.contentPreview || "")}</p>
      <div class="brief-list">
        ${(detail.chunks || []).map((chunk) => `
          <article class="brief-item">
            <strong>#${escapeHtml(chunk.chunkIndex)}</strong>
            <span>dim ${escapeHtml(chunk.embeddingDimension ?? 0)}</span>
            <p>${escapeHtml(chunk.contentPreview || "")}</p>
          </article>
        `).join("")}
      </div>
    `;
    updateModeText();
  } catch (error) {
    toast(`详情读取失败：${error.message}`, "error");
  }
}

async function deleteDocument(documentId, fileName) {
  if (!window.confirm(`确认删除文档「${fileName}」？该操作会同步删除 chunks 和内存索引。`)) {
    return;
  }
  try {
    await requestJson(`/api/documents/${encodeURIComponent(documentId)}`, { method: "DELETE" });
    if (state.selectedDocumentId === documentId) {
      state.selectedDocumentId = null;
      clearDocumentDetail();
    }
    await loadDocuments();
    toast("文档已删除");
  } catch (error) {
    toast(`删除失败：${error.message}`, "error");
  }
}

function clearDocumentDetail() {
  els.detailTitle.textContent = "未选择文档";
  els.detailMeta.textContent = "查看文档详情后会显示 chunks 摘要";
  els.documentDetail.className = "detail-box empty-state";
  els.documentDetail.textContent = "点击文档列表中的“详情”查看 contentPreview 和 chunk 摘要。";
}

async function uploadDocument() {
  const file = els.fileInput.files[0];
  if (!file) {
    toast("请先选择文件", "error");
    return;
  }
  const formData = new FormData();
  formData.append("file", file);
  setBusy(els.uploadBtn, true, "索引中");
  setStatus(els.uploadStatus, "正在上传并生成 embedding...", "loading");
  try {
    const result = await requestJson("/api/documents/upload", { method: "POST", body: formData });
    state.selectedDocumentId = result.documentId;
    els.fileInput.value = "";
    els.fileNameText.textContent = "选择知识库文件";
    await loadDocuments();
    await loadDocumentDetail(result.documentId);
    toast("上传成功");
    setStatus(els.uploadStatus, "上传成功，知识库已刷新。", "success");
  } catch (error) {
    setStatus(els.uploadStatus, `上传失败：${error.message}`, "error");
    toast(`上传失败：${error.message}`, "error");
  } finally {
    setBusy(els.uploadBtn, false, "上传并索引");
  }
}

async function loadSessions() {
  try {
    const sessions = await requestJson("/api/chat/sessions");
    renderSessions(Array.isArray(sessions) ? sessions : []);
  } catch (error) {
    els.sessionList.innerHTML = '<div class="empty-state">暂无会话列表。</div>';
  }
}

function renderSessions(sessions) {
  if (sessions.length === 0) {
    els.sessionList.innerHTML = '<div class="empty-state">暂无历史会话。</div>';
    return;
  }
  els.sessionList.innerHTML = sessions.map((session) => `
    <button class="session-item" type="button" data-session="${escapeHtml(session.sessionId)}">
      <strong>${escapeHtml(session.sessionId)}</strong>
      <span>${escapeHtml(session.messageCount ?? 0)} messages · ${escapeHtml(formatDateTime(session.lastMessageAt))}</span>
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

async function clearMemory() {
  const sessionId = currentSessionId();
  try {
    await requestJson(`/api/chat/memory/${encodeURIComponent(sessionId)}`, { method: "DELETE" });
    await loadSessions();
    toast("会话记忆已清空");
  } catch (error) {
    toast(`清空失败：${error.message}`, "error");
  }
}

function newSession() {
  const id = `session-${Date.now().toString(36)}`;
  els.sessionIdInput.value = id;
  currentSessionId();
  toast("已创建本地会话 ID");
}

function updateMode(mode) {
  state.mode = mode;
  for (const [name, button] of Object.entries(modeButtons)) {
    button.classList.toggle("active", name === mode);
  }
  updateModeText();
}

function updateModeText() {
  const doc = selectedDocument();
  if (state.mode === "single") {
    els.activeContextText.textContent = doc ? `单文档 RAG：${doc.fileName}` : "单文档 RAG 需要先选择文档。";
  } else if (state.mode === "global") {
    els.activeContextText.textContent = "全局 RAG 会检索全部已索引文档。";
  } else {
    els.activeContextText.textContent = "普通聊天只调用 generation provider，不检索知识库。";
  }
}

function buildAskRequest(question) {
  if (state.mode === "single") {
    return { url: "/api/knowledge/ask", body: { documentId: state.selectedDocumentId, question } };
  }
  if (state.mode === "global") {
    return { url: "/api/knowledge/ask-global", body: { sessionId: currentSessionId(), question } };
  }
  return { url: "/api/chat", body: { sessionId: currentSessionId(), message: question } };
}

async function ask() {
  const question = els.questionInput.value.trim();
  if (!question || state.sending) return;
  if (state.mode === "single" && !state.selectedDocumentId) {
    toast("请先选择一个文档", "error");
    return;
  }

  const request = buildAskRequest(question);
  state.sending = true;
  setBusy(els.askBtn, true, "生成中");
  setStatus(els.askStatus, "正在生成回答...", "loading");
  appendMessage("user", question);
  els.questionInput.value = "";
  renderChunks([]);

  try {
    const result = await requestJson(request.url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request.body)
    });
    appendMessage("assistant", result.reply || "", result.model);
    renderChunks(Array.isArray(result.retrievedChunks) ? result.retrievedChunks : []);
    await loadSessions();
    setStatus(els.askStatus, "回答完成。", "success");
    toast("回答完成");
  } catch (error) {
    const friendly = error.message.includes("当前没有任何可检索文档")
      ? "请先上传文档，再使用全局知识库问答。"
      : error.message;
    appendMessage("assistant", friendly, "error");
    setStatus(els.askStatus, `请求失败：${friendly}`, "error");
    toast(friendly, "error");
  } finally {
    state.sending = false;
    setBusy(els.askBtn, false, "发送");
  }
}

function appendMessage(role, content, model = "") {
  if (els.messageList.querySelector(".empty-state")) {
    els.messageList.innerHTML = "";
  }
  const item = document.createElement("article");
  item.className = `message ${role}`;
  item.innerHTML = `
    <div class="message-role">${role === "user" ? "USER" : "AI"} ${model ? `<span>${escapeHtml(model)}</span>` : ""}</div>
    <p>${escapeHtml(content)}</p>
  `;
  els.messageList.appendChild(item);
  els.messageList.scrollTop = els.messageList.scrollHeight;
}

function renderChunks(chunks) {
  if (!chunks || chunks.length === 0) {
    els.chunksList.innerHTML = "";
    return;
  }
  els.chunksList.innerHTML = chunks.map((chunk) => `
    <article class="chunk-card">
      <div>
        <strong>${escapeHtml(chunk.fileName || "-")}</strong>
        <span>score ${formatScore(chunk.score)}</span>
      </div>
      <p>${escapeHtml(chunk.contentPreview || "")}</p>
    </article>
  `).join("");
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
els.globalModeBtn.addEventListener("click", () => updateMode("global"));
els.singleModeBtn.addEventListener("click", () => updateMode("single"));
els.chatModeBtn.addEventListener("click", () => updateMode("chat"));
els.askBtn.addEventListener("click", ask);
els.questionInput.addEventListener("keydown", (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key === "Enter") ask();
});

currentSessionId();
updateMode("global");
refreshAll();
