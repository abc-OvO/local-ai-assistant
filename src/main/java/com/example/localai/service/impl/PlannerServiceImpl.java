package com.example.localai.service.impl;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.config.RagProperties;
import com.example.localai.model.QueryIntent;
import com.example.localai.service.PlannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlannerServiceImpl implements PlannerService {

    private static final List<String> DIRECT_CHAT_PHRASES = List.of(
            "你好",
            "hello",
            "hi",
            "你是谁"
    );

    private static final List<String> KNOWLEDGE_SEARCH_PHRASES = List.of(
            "上传",
            "文档",
            "文件",
            "pdf",
            "资料",
            "知识库",
            "根据",
            "依据",
            "总结这个",
            "这篇",
            "附件",
            "chunk",
            "来源"
    );

    private final AiChatClientRouter aiChatClientRouter;

    private final RagProperties ragProperties;

    @Override
    public QueryIntent plan(String question) {
        long start = System.currentTimeMillis();
        int questionLength = question == null ? 0 : question.length();

        if (!ragProperties.getAuto().isEnabled()) {
            return logAndReturn(QueryIntent.KNOWLEDGE_SEARCH, questionLength, start, false, "auto-disabled");
        }

        QueryIntent ruleIntent = ruleBasedIntent(question);
        if (ruleIntent != null) {
            return logAndReturn(ruleIntent, questionLength, start, false, "rule");
        }

        if (!ragProperties.getPlanner().isEnabled()) {
            return logAndReturn(fallbackIntent(), questionLength, start, true, "planner-disabled");
        }

        try {
            QueryIntent intent = parseIntent(aiChatClientRouter.generate(buildPlannerPrompt(question)));
            if (intent == null) {
                return logAndReturn(fallbackIntent(), questionLength, start, true, "invalid-output");
            }
            return logAndReturn(intent, questionLength, start, false, "llm");
        } catch (RuntimeException ex) {
            System.out.println("[QueryPlanner] failed, questionLength=" + questionLength
                    + ", fallback=true"
                    + ", plannerCostMs=" + (System.currentTimeMillis() - start)
                    + ", error=" + ex.getMessage());
            return fallbackIntent();
        }
    }

    private QueryIntent ruleBasedIntent(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }

        String normalized = question.trim().toLowerCase(Locale.ROOT);
        if (DIRECT_CHAT_PHRASES.stream().anyMatch(phrase -> normalized.equals(phrase) || normalized.equals(phrase + "。"))) {
            return QueryIntent.DIRECT_CHAT;
        }
        if (KNOWLEDGE_SEARCH_PHRASES.stream().anyMatch(normalized::contains)) {
            return QueryIntent.KNOWLEDGE_SEARCH;
        }
        return null;
    }

    private QueryIntent fallbackIntent() {
        QueryIntent configuredIntent = parseIntent(ragProperties.getPlanner().getFallbackIntent());
        return configuredIntent == null ? QueryIntent.KNOWLEDGE_SEARCH : configuredIntent;
    }

    private QueryIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim()
                .replace("```", "")
                .toUpperCase(Locale.ROOT)
                .trim();
        if ("KNOWLEDGE_SEARCH".equals(normalized)) {
            return QueryIntent.KNOWLEDGE_SEARCH;
        }
        if ("DIRECT_CHAT".equals(normalized)) {
            return QueryIntent.DIRECT_CHAT;
        }
        return null;
    }

    private String buildPlannerPrompt(String question) {
        return """
            你是一个轻量级 RAG Query Planner。
            请判断用户问题是否需要检索本地知识库。

            只能输出以下两个标签之一，不要解释：
            DIRECT_CHAT
            KNOWLEDGE_SEARCH

            DIRECT_CHAT 示例：
            - 你好
            - 你是谁
            - 帮我写一个快速排序
            - 解释一下 Java Stream
            - 翻译这句话
            - 写一段简历描述

            KNOWLEDGE_SEARCH 示例：
            - 总结我上传的文档
            - 根据知识库回答
            - 这个 PDF 讲了什么
            - 文档中提到的系统架构是什么
            - 我上传的文件里有没有说到 MMR
            - 根据资料回答
            - 从文档中找
            - 按照附件内容

            用户问题：
            %s
            """.formatted(question);
    }

    private QueryIntent logAndReturn(QueryIntent intent, int questionLength, long start, boolean fallback, String source) {
        System.out.println("[QueryPlanner] mode=auto"
                + ", intent=" + intent
                + ", ragUsed=" + (intent == QueryIntent.KNOWLEDGE_SEARCH)
                + ", questionLength=" + questionLength
                + ", plannerCostMs=" + (System.currentTimeMillis() - start)
                + ", fallback=" + fallback
                + ", source=" + source);
        return intent;
    }
}
