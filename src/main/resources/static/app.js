const state = {
  documents: [],
  selectedDocumentId: null,
  mode: "single",
  sessionId: "default"
};

const modeMeta = {
  single: {
    label: "单文档",
    metric: "RAG",
    description: "使用当前选中文档进行检索增强问答",
    activeText: "单文档模式需要先选择文档",
    loading: "正在检索当前文档..."
  },
  global: {
    label: "全局知识库",
    metric: "GLOBAL",
    description: "跨全部已索引文档执行多文档检索增强问答",
    activeText: "全局模式不需要选择文档",
    loading: "正在执行全局检索..."
  },
  chat: {
    label: "普通聊天",
    metric: "CHAT",
    description: "直接调用本地聊天模型，不使用知识库检索",
    activeText: "普通聊天不会读取文档",
    loading: "正在调用聊天模型..."
  }
};

const els = {
  documentCountText: document.querySelector("#documentCountText"),
  currentModeText: document.querySelector("#currentModeText"),
  refreshDocumentsBtn: document.querySelector("#refreshDocumentsBtn"),
  fileInput: document.querySelector("#fileInput"),
  fileNameText: document.querySelector("#fileNameText"),
  uploadBtn: document.querySelector("#uploadBtn"),
  uploadStatus: document.querySelector("#uploadStatus"),
  uploadResult: document.querySelector("#uploadResult"),
  documentStatus: document.querySelector("#documentStatus"),
  documentList: document.querySelector("#documentList"),
  selectedDocumentCard: document.querySelector("#selectedDocumentCard"),
  singleModeBtn: document.querySelector("#singleModeBtn"),
  globalModeBtn: document.querySelector("#globalModeBtn"),
  chatModeBtn: document.querySelector("#chatModeBtn"),
  modeDescription: document.querySelector("#modeDescription"),
  activeDocumentText: document.querySelector("#activeDocumentText"),
  sessionIdInput: document.querySelector("#sessionIdInput"),
  clearMemoryBtn: document.querySelector("#clearMemoryBtn"),
  questionInput: document.querySelector("#questionInput"),
  askBtn: document.querySelector("#askBtn"),
  askStatus: document.querySelector("#askStatus"),
  answerBox: document.querySelector("#answerBox"),
  modelText: document.querySelector("#modelText"),
  chunkCountText: document.querySelector("#chunkCountText"),
  chunksList: document.querySelector("#chunksList")
};

const modeButtons = {
  single: els.singleModeBtn,
  global: els.globalModeBtn,
  chat: els.chatModeBtn
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

function setStatus(el, message, type = "") {
  el.textContent = message;
  el.className = `status ${type}`.trim();
}

function setBusy(button, busy, text) {
  button.disabled = busy;
  if (text) {
    const label = button.querySelector("span");
    if (label) {
      label.textContent = text;
    } else {
      button.textContent = text;
    }
  }
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
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function formatScore(score) {
  const value = Number(score);
  return Number.isFinite(value) ? value.toFixed(4) : "-";
}

function currentDocument() {
  return state.documents.find((doc) => doc.documentId === state.selectedDocumentId) || null;
}

function updateMode(mode) {
  state.mode = mode;

  for (const [name, button] of Object.entries(modeButtons)) {
    const isActive = name === mode;
    button.classList.toggle("active", isActive);
    button.setAttribute("aria-selected", String(isActive));
  }

  els.currentModeText.textContent = modeMeta[mode].metric;
  els.modeDescription.textContent = modeMeta[mode].description;
  renderSelectedDocument();
}

function renderDocuments() {
  els.documentList.innerHTML = "";
  els.documentCountText.textContent = String(state.documents.length);

  if (state.documents.length === 0) {
    els.documentList.innerHTML = '<div class="empty-state">暂无文档。上传 txt、md 或 pdf 后会出现在这里。</div>';
    renderSelectedDocument();
    return;
  }

  for (const doc of state.documents) {
    const item = document.createElement("button");
    item.type = "button";
    item.className = doc.documentId === state.selectedDocumentId
      ? "document-item selected"
      : "document-item";
    item.innerHTML = `
      <span class="doc-name">${escapeHtml(doc.fileName)}</span>
      <span class="doc-meta">
        <span>${escapeHtml(doc.fileType || "-")}</span>
        <span>${escapeHtml(doc.contentLength ?? "-")} chars</span>
      </span>
      <span class="doc-id">${escapeHtml(doc.documentId)}</span>
      <span class="doc-time">${escapeHtml(formatDateTime(doc.uploadTime))}</span>
    `;
    item.addEventListener("click", () => selectDocument(doc.documentId));
    els.documentList.appendChild(item);
  }

  renderSelectedDocument();
}

function renderSelectedDocument() {
  const doc = currentDocument();

  if (!doc) {
    els.selectedDocumentCard.className = "selected-document empty";
    els.selectedDocumentCard.textContent = "尚未选择文档";
    els.activeDocumentText.textContent = modeMeta[state.mode].activeText;
    return;
  }

  els.selectedDocumentCard.className = "selected-document";
  els.selectedDocumentCard.innerHTML = `
    <strong>${escapeHtml(doc.fileName)}</strong>
    <span>${escapeHtml(doc.fileType || "-")} · ${escapeHtml(doc.contentLength ?? "-")} chars</span>
    <code>${escapeHtml(doc.documentId)}</code>
  `;

  if (state.mode === "single") {
    els.activeDocumentText.textContent = `当前文档：${doc.fileName}`;
  } else if (state.mode === "global") {
    els.activeDocumentText.textContent = `已选：${doc.fileName}，全局模式会检索全部文档`;
  } else {
    els.activeDocumentText.textContent = "普通聊天不会读取文档";
  }
}

function selectDocument(documentId) {
  state.selectedDocumentId = documentId;
  renderDocuments();
  setStatus(els.documentStatus, "已切换当前选中文档。", "success");
}

function currentSessionId() {
  const sessionId = els.sessionIdInput.value.trim() || "default";
  state.sessionId = sessionId;
  return sessionId;
}

async function loadDocuments() {
  setStatus(els.documentStatus, "正在同步文档列表...", "loading");
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

async function uploadDocument() {
  const file = els.fileInput.files[0];
  if (!file) {
    setStatus(els.uploadStatus, "请先选择一个 txt、md 或 pdf 文件。", "error");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);

  setBusy(els.uploadBtn, true, "索引中...");
  setStatus(els.uploadStatus, "正在上传并生成可检索文本块...", "loading");
  els.uploadResult.classList.add("hidden");
  els.uploadResult.innerHTML = "";

  try {
    const result = await requestJson("/api/documents/upload", {
      method: "POST",
      body: formData
    });

    els.uploadResult.innerHTML = `
      <strong>上传成功</strong>
      <span>${escapeHtml(result.fileName)}</span>
      <code>${escapeHtml(result.documentId)}</code>
    `;
    els.uploadResult.classList.remove("hidden");
    setStatus(els.uploadStatus, "上传成功，文档列表已刷新。", "success");
    els.fileInput.value = "";
    els.fileNameText.textContent = "选择知识库文件";
    state.selectedDocumentId = result.documentId;
    await loadDocuments();
  } catch (error) {
    setStatus(els.uploadStatus, `上传失败：${error.message}`, "error");
  } finally {
    setBusy(els.uploadBtn, false, "上传并索引");
  }
}

function buildAskRequest(question) {
  if (state.mode === "single") {
    return {
      url: "/api/knowledge/ask",
      body: { documentId: state.selectedDocumentId, question }
    };
  }

  if (state.mode === "global") {
    return {
      url: "/api/knowledge/ask-global",
      body: { sessionId: currentSessionId(), question }
    };
  }

  return {
    url: "/api/chat",
    body: { sessionId: currentSessionId(), message: question }
  };
}

async function clearMemory() {
  const sessionId = currentSessionId();
  setBusy(els.clearMemoryBtn, true, "清空中...");
  setStatus(els.askStatus, `正在清空 session ${sessionId} 的记忆...`, "loading");

  try {
    await requestJson(`/api/chat/memory/${encodeURIComponent(sessionId)}`, {
      method: "DELETE"
    });
    setStatus(els.askStatus, `已清空 session ${sessionId} 的记忆。`, "success");
  } catch (error) {
    setStatus(els.askStatus, `清空失败：${error.message}`, "error");
  } finally {
    setBusy(els.clearMemoryBtn, false, "清空记忆");
  }
}

async function askKnowledge() {
  const question = els.questionInput.value.trim();
  if (!question) {
    setStatus(els.askStatus, "请输入问题。", "error");
    return;
  }

  if (state.mode === "single" && !state.selectedDocumentId) {
    setStatus(els.askStatus, "单文档问答需要先在左侧选择文档。", "error");
    return;
  }

  const request = buildAskRequest(question);

  setBusy(els.askBtn, true, "生成中...");
  setStatus(els.askStatus, modeMeta[state.mode].loading, "loading");
  renderAnswer(null);
  renderChunks([]);
  els.modelText.textContent = "model: -";

  try {
    const result = await requestJson(request.url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request.body)
    });

    const chunks = Array.isArray(result.retrievedChunks) ? result.retrievedChunks : [];
    renderAnswer(result.reply || "");
    renderChunks(chunks);
    els.modelText.textContent = `model: ${result.model || "-"}`;
    setStatus(
      els.askStatus,
      state.mode === "chat" ? "聊天回答完成。" : `回答完成，命中 ${chunks.length} 个片段。`,
      "success"
    );
  } catch (error) {
    const message = error.message.includes("当前没有任何可检索文档")
      ? "请先上传文档，再使用全局知识库问答。"
      : `请求失败：${error.message}`;
    setStatus(els.askStatus, message, "error");
    els.answerBox.className = "answer-box empty";
    els.answerBox.textContent = message;
  } finally {
    setBusy(els.askBtn, false, "发送问题");
  }
}

function renderAnswer(reply) {
  if (reply === null) {
    els.answerBox.className = "answer-box empty";
    els.answerBox.textContent = "等待模型生成回答...";
    return;
  }

  if (!reply) {
    els.answerBox.className = "answer-box empty";
    els.answerBox.textContent = "AI 回答会显示在这里。";
    return;
  }

  els.answerBox.className = "answer-box";
  els.answerBox.innerHTML = escapeHtml(reply);
}

function renderChunks(chunks) {
  els.chunkCountText.textContent = `chunks: ${chunks.length}`;

  if (chunks.length === 0) {
    els.chunksList.innerHTML = '<div class="empty-state">暂无检索片段。</div>';
    return;
  }

  els.chunksList.innerHTML = chunks.map((chunk) => `
    <article class="chunk-card">
      <div class="chunk-topline">
        <strong>${escapeHtml(chunk.fileName || "-")}</strong>
        <span>${formatScore(chunk.score)}</span>
      </div>
      <p>${escapeHtml(chunk.contentPreview || "")}</p>
      <div class="chunk-meta">
        <span>chunk ${escapeHtml(chunk.chunkIndex ?? "-")}</span>
        <code>${escapeHtml(chunk.documentId || "-")}</code>
      </div>
    </article>
  `).join("");
}

els.fileInput.addEventListener("change", () => {
  const file = els.fileInput.files[0];
  els.fileNameText.textContent = file ? file.name : "选择知识库文件";
});
els.refreshDocumentsBtn.addEventListener("click", loadDocuments);
els.uploadBtn.addEventListener("click", uploadDocument);
els.singleModeBtn.addEventListener("click", () => updateMode("single"));
els.globalModeBtn.addEventListener("click", () => updateMode("global"));
els.chatModeBtn.addEventListener("click", () => updateMode("chat"));
els.sessionIdInput.addEventListener("input", currentSessionId);
els.clearMemoryBtn.addEventListener("click", clearMemory);
els.askBtn.addEventListener("click", askKnowledge);
els.questionInput.addEventListener("keydown", (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
    askKnowledge();
  }
});

updateMode("single");
loadDocuments();
