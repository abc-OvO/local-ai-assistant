package com.example.localai.service.impl;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.config.AppProperties;
import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.KnowledgeAskResponse;
import com.example.localai.dto.RetrievedChunkInfo;
import com.example.localai.model.DocumentChunk;
import com.example.localai.model.DocumentRecord;
import com.example.localai.service.DocumentService;
import com.example.localai.service.EmbeddingService;
import com.example.localai.service.ConversationMemoryService;
import com.example.localai.service.KnowledgeService;
import com.example.localai.service.QueryRewriteService;
import com.example.localai.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final DocumentService documentService;

    private final AiChatClientRouter aiChatClientRouter;

    private final AppProperties appProperties;

    private final EmbeddingService embeddingService;

    private final RetrievalService retrievalService;

    private final ConversationMemoryService conversationMemoryService;

    private final OllamaProperties ollamaProperties;

    private final QueryRewriteService queryRewriteService;

    @Override
    public KnowledgeAskResponse ask(String documentId, String question) {
        long start = System.currentTimeMillis();

        DocumentRecord document = documentService.getDocument(documentId);
        String retrievalQuery = queryRewriteService.rewrite(question);
        List<Double> questionEmbedding = embeddingService.embed(retrievalQuery);
        List<DocumentChunk> retrievedChunks = retrievalService.retrieve(
                documentId,
                retrievalQuery,
                questionEmbedding,
                appProperties.getRetrievalTopK()
        );
        String context = buildContext(retrievedChunks);
        String prompt = buildPrompt(context, question);

        System.out.println("[KnowledgeAsk] documentId=" + documentId
                + ", questionLength=" + question.length()
                + ", retrievalQueryLength=" + retrievalQuery.length()
                + ", retrievedChunks=" + retrievedChunks.size()
                + ", contextLength=" + context.length()
                + ", promptLength=" + prompt.length());

        String reply = aiChatClientRouter.generate(prompt);
        String model = aiChatClientRouter.currentModel();

        long cost = System.currentTimeMillis() - start;
        System.out.println("[KnowledgeAsk] success, costMs=" + cost);

        return new KnowledgeAskResponse(
                document.getDocumentId(),
                document.getFileName(),
                model,
                reply,
                toRetrievedChunkInfos(retrievedChunks)
        );
    }

    @Override
    public KnowledgeAskResponse askGlobal(String question) {
        return askGlobal(null, question);
    }

    @Override
    public KnowledgeAskResponse askGlobal(String sessionId, String question) {
        long start = System.currentTimeMillis();
        String normalizedSessionId = conversationMemoryService.normalizeSessionId(sessionId);

        String retrievalQuery = queryRewriteService.rewrite(question);
        List<Double> questionEmbedding = embeddingService.embed(retrievalQuery);
        List<DocumentChunk> retrievedChunks = retrievalService.retrieveGlobal(
                retrievalQuery,
                questionEmbedding,
                appProperties.getRetrievalTopK()
        );
        String context = buildContext(retrievedChunks);
        String history = conversationMemoryService.formatHistory(
                conversationMemoryService.getRecentMessages(normalizedSessionId)
        );
        String prompt = buildGlobalPrompt(history, context, question);

        System.out.println("[KnowledgeAskGlobal] questionLength=" + question.length()
                + ", retrievalQueryLength=" + retrievalQuery.length()
                + ", sessionId=" + normalizedSessionId
                + ", retrievedChunks=" + retrievedChunks.size()
                + ", contextLength=" + context.length()
                + ", promptLength=" + prompt.length());

        String reply = aiChatClientRouter.generate(prompt);
        String model = aiChatClientRouter.currentModel();
        conversationMemoryService.appendSuccessfulTurn(normalizedSessionId, question, reply);
        int historyTurns = conversationMemoryService.historyTurns(normalizedSessionId);

        System.out.println("[RagMemory] endpoint=/api/knowledge/ask-global"
                + ", sessionId=" + normalizedSessionId
                + ", memoryEnabled=" + conversationMemoryService.isEnabled()
                + ", historyTurns=" + historyTurns
                + ", retrievedChunks=" + retrievedChunks.size()
                + ", promptLength=" + prompt.length()
                + ", retrievalQueryLength=" + retrievalQuery.length()
                + ", embeddingProvider=ollama"
                + ", embeddingModel=" + ollamaProperties.getEmbeddingModel()
                + ", generationProvider=" + aiChatClientRouter.currentProvider());

        long cost = System.currentTimeMillis() - start;
        System.out.println("[KnowledgeAskGlobal] success, costMs=" + cost);

        DocumentChunk firstChunk = retrievedChunks.get(0);
        return new KnowledgeAskResponse(
                firstChunk.getDocumentId(),
                firstChunk.getFileName(),
                model,
                reply,
                toRetrievedChunkInfos(retrievedChunks),
                normalizedSessionId,
                conversationMemoryService.isEnabled(),
                historyTurns
        );
    }

    private String buildContext(List<DocumentChunk> retrievedChunks) {
        String context = retrievedChunks.stream()
                .map(chunk -> "片段 " + chunk.getChunkIndex() + "，相似度 " + formatScore(chunk.getScore()) + "：\n" + chunk.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        int maxLength = appProperties.getMaxContextLength();
        if (context.length() <= maxLength) {
            return context;
        }
        // 检索后仍保留最大上下文限制，避免 prompt 过长。
        return context.substring(0, maxLength);
    }

    private String buildPrompt(String context, String question) {
        return """
            你是一个文档问答助手。请严格依据下面检索到的文档片段回答问题，不要编造。
            如果片段中没有相关信息，请明确回答“文档中未提及”。
            回答尽量简洁，优先直接提取文档中的事实。

            检索片段：
            %s

            问题：
            %s
            """.formatted(context, question);
    }

    private String buildGlobalPrompt(String history, String context, String question) {
        return """
            你是一个基于本地知识库的问答助手。
            请优先依据【知识库片段】回答问题；如果问题涉及“刚才”“它”“这个设计”等上下文指代，可以参考【最近对话历史】理解用户意图。
            如果知识库片段中没有相关信息，请明确说明“知识库中未提及”，不要编造。

            【最近对话历史】
            %s

            【知识库片段】
            %s

            【用户当前问题】
            %s

            请给出准确、简洁的回答。
            """.formatted(history, context, question);
    }

    private List<RetrievedChunkInfo> toRetrievedChunkInfos(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new RetrievedChunkInfo(
                        chunk.getChunkId(),
                        chunk.getDocumentId(),
                        chunk.getFileName(),
                        chunk.getChunkIndex(),
                        chunk.getScore(),
                        chunk.getVectorScore(),
                        chunk.getKeywordScore(),
                        chunk.getFinalScore(),
                        buildPreview(chunk.getContent())
                ))
                .toList();
    }

    private String buildPreview(String content) {
        int previewLength = Math.min(content.length(), 120);
        return content.substring(0, previewLength);
    }

    private String formatScore(Double score) {
        if (score == null) {
            return "0.0000";
        }
        return String.format("%.4f", score);
    }
}
