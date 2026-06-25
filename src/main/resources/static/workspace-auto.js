(function (root) {
  function normalizePlannerIntent(value) {
    return value === "DIRECT_CHAT" ? "DIRECT_CHAT" : "KNOWLEDGE_SEARCH";
  }

  function buildAutoRoute(intent, sessionId, question) {
    const normalizedIntent = normalizePlannerIntent(intent);
    if (normalizedIntent === "DIRECT_CHAT") {
      return {
        url: "/api/chat",
        body: { sessionId, message: question },
        metadata: { mode: "auto", intent: normalizedIntent, ragUsed: false }
      };
    }
    return {
      url: "/api/knowledge/ask-global",
      body: { sessionId, question },
      metadata: { mode: "auto", intent: normalizedIntent, ragUsed: true }
    };
  }

  function chunksForAssistant(metadata, result) {
    if (metadata && metadata.ragUsed === false) {
      return [];
    }
    return Array.isArray(result && result.retrievedChunks) ? result.retrievedChunks : [];
  }

  const api = {
    normalizePlannerIntent,
    buildAutoRoute,
    chunksForAssistant
  };

  root.WorkspaceAuto = api;
  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
})(typeof globalThis !== "undefined" ? globalThis : window);
