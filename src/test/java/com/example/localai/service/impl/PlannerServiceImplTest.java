package com.example.localai.service.impl;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.config.RagProperties;
import com.example.localai.exception.BusinessException;
import com.example.localai.model.QueryIntent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlannerServiceImplTest {

    @Test
    void greetingUsesDirectChatRule() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        PlannerServiceImpl service = new PlannerServiceImpl(router, new RagProperties());

        assertThat(service.plan("你好")).isEqualTo(QueryIntent.DIRECT_CHAT);
        verify(router, never()).generate(anyString());
    }

    @Test
    void documentQuestionUsesKnowledgeSearchRule() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        PlannerServiceImpl service = new PlannerServiceImpl(router, new RagProperties());

        assertThat(service.plan("请根据资料总结这个 PDF")).isEqualTo(QueryIntent.KNOWLEDGE_SEARCH);
        verify(router, never()).generate(anyString());
    }

    @Test
    void plannerFailureFallsBackToKnowledgeSearch() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        when(router.generate(anyString())).thenThrow(new BusinessException(502, "provider unavailable"));
        PlannerServiceImpl service = new PlannerServiceImpl(router, new RagProperties());

        assertThat(service.plan("这个设计有什么优缺点")).isEqualTo(QueryIntent.KNOWLEDGE_SEARCH);
    }

    @Test
    void invalidPlannerOutputFallsBackToKnowledgeSearch() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        when(router.generate(anyString())).thenReturn("我认为应该 DIRECT_CHAT");
        PlannerServiceImpl service = new PlannerServiceImpl(router, new RagProperties());

        assertThat(service.plan("这个设计有什么优缺点")).isEqualTo(QueryIntent.KNOWLEDGE_SEARCH);
    }

    @Test
    void disabledPlannerUsesRulesThenFallback() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        RagProperties properties = new RagProperties();
        properties.getPlanner().setEnabled(false);
        PlannerServiceImpl service = new PlannerServiceImpl(router, properties);

        assertThat(service.plan("解释一下 Java Stream")).isEqualTo(QueryIntent.KNOWLEDGE_SEARCH);
        assertThat(service.plan("文档里提到了什么")).isEqualTo(QueryIntent.KNOWLEDGE_SEARCH);
        verify(router, never()).generate(anyString());
    }
}
