package com.example.localai.service.impl;

import com.example.localai.client.OllamaClient;
import com.example.localai.config.AppProperties;
import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.KnowledgeAskResponse;
import com.example.localai.dto.OllamaGenerateResponse;
import com.example.localai.model.DocumentRecord;
import com.example.localai.service.DocumentService;
import com.example.localai.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final DocumentService documentService;

    private final OllamaClient ollamaClient;

    private final OllamaProperties ollamaProperties;

    private final AppProperties appProperties;

    @Override
    public KnowledgeAskResponse ask(String documentId, String question) {
        DocumentRecord document = documentService.getDocument(documentId);
        String context = buildContext(document.getContent());
        String prompt = buildPrompt(context, question);

        OllamaGenerateResponse ollamaResponse = ollamaClient.generate(prompt);
        String model = ollamaResponse.getModel() == null ? ollamaProperties.getModel() : ollamaResponse.getModel();

        return new KnowledgeAskResponse(
                document.getDocumentId(),
                document.getFileName(),
                model,
                ollamaResponse.getResponse()
        );
    }

    private String buildContext(String content) {
        int maxLength = appProperties.getMaxContextLength();
        if (content.length() <= maxLength) {
            return content;
        }
        // 第二阶段先采用简单截断，后续可替换为检索或向量召回。
        return content.substring(0, maxLength);
    }

    private String buildPrompt(String context, String question) {
        return """
                你是一个文档问答助手。请严格依据下面提供的文档内容回答问题，不要编造。
                如果文档中没有提及相关信息，请明确回答“文档中未提及”。

                文档内容：
                %s

                问题：
                %s
                """.formatted(context, question);
    }
}
