const assert = require("node:assert/strict");
const WorkspaceAuto = require("../../../src/main/resources/static/workspace-auto.js");

{
  const route = WorkspaceAuto.buildAutoRoute("DIRECT_CHAT", "s1", "你好");
  const chunks = WorkspaceAuto.chunksForAssistant(route.metadata, {
    retrievedChunks: [{ chunkId: "c1" }]
  });

  assert.equal(route.url, "/api/chat");
  assert.equal(route.metadata.ragUsed, false);
  assert.deepEqual(chunks, []);
}

{
  const route = WorkspaceAuto.buildAutoRoute("KNOWLEDGE_SEARCH", "s1", "总结文档");
  const chunks = WorkspaceAuto.chunksForAssistant(route.metadata, {
    retrievedChunks: [{ chunkId: "c1" }]
  });

  assert.equal(route.url, "/api/knowledge/ask-global");
  assert.equal(route.metadata.ragUsed, true);
  assert.equal(chunks.length, 1);
}

{
  assert.equal(WorkspaceAuto.normalizePlannerIntent("unexpected"), "KNOWLEDGE_SEARCH");
}
