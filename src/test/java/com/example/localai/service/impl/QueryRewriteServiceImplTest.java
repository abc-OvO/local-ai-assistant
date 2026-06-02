package com.example.localai.service.impl;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.config.RagProperties;
import com.example.localai.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryRewriteServiceImplTest {

    @Test
    void rewriteFallsBackToOriginalQuestionWhenProviderFails() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        when(router.generate(anyString())).thenThrow(new BusinessException(502, "provider unavailable"));
        QueryRewriteServiceImpl service = new QueryRewriteServiceImpl(router, new RagProperties());

        String question = "这个项目使用了什么技术？";

        assertThat(service.rewrite(question)).isEqualTo(question);
    }
}
