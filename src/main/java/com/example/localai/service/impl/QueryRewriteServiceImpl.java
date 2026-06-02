package com.example.localai.service.impl;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.config.RagProperties;
import com.example.localai.service.QueryRewriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QueryRewriteServiceImpl implements QueryRewriteService {

    private final AiChatClientRouter aiChatClientRouter;

    private final RagProperties ragProperties;

    @Override
    public String rewrite(String question) {
        if (!ragProperties.getRewrite().isEnabled() || !StringUtils.hasText(question)) {
            return question;
        }

        long start = System.currentTimeMillis();
        int questionLength = question.length();
        try {
            String rewrittenQuery = sanitize(aiChatClientRouter.generate(buildRewritePrompt(question)));
            if (!StringUtils.hasText(rewrittenQuery)) {
                System.out.println("[QueryRewrite] empty result, fallback=true, questionLength=" + questionLength);
                return question;
            }
            System.out.println("[QueryRewrite] success, questionLength=" + questionLength
                    + ", rewrittenQueryLength=" + rewrittenQuery.length()
                    + ", costMs=" + (System.currentTimeMillis() - start));
            return rewrittenQuery;
        } catch (RuntimeException ex) {
            System.out.println("[QueryRewrite] failed, fallback=true, questionLength=" + questionLength
                    + ", costMs=" + (System.currentTimeMillis() - start)
                    + ", error=" + ex.getMessage());
            return question;
        }
    }

    private String buildRewritePrompt(String question) {
        return """
            你是 RAG 检索 query 改写器。
            请把用户问题改写为适合向量检索和关键词检索的短 query。
            要求：
            - 只输出改写后的 query
            - 保留关键实体、技术名词、文件名、时间和约束
            - 偏关键词化，尽量短
            - 不要解释，不要加引号，不要 markdown

            用户问题：
            %s
            """.formatted(question);
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String rewrittenQuery = value
                .replace("```", " ")
                .replace("\"", " ")
                .replace("'", " ")
                .replaceAll("\\s+", " ")
                .trim();

        int maxLength = ragProperties.getRewrite().getMaxLength();
        if (rewrittenQuery.length() <= maxLength) {
            return rewrittenQuery;
        }
        return rewrittenQuery.substring(0, maxLength).trim();
    }
}
