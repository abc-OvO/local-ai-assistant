package com.example.localai.service;

import com.example.localai.dto.KnowledgeAskResponse;

public interface KnowledgeService {

    KnowledgeAskResponse ask(String documentId, String question);

    KnowledgeAskResponse askGlobal(String question);
}
