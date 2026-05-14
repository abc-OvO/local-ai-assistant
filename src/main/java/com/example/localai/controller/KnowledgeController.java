package com.example.localai.controller;

import com.example.localai.common.Result;
import com.example.localai.dto.GlobalKnowledgeAskRequest;
import com.example.localai.dto.KnowledgeAskRequest;
import com.example.localai.dto.KnowledgeAskResponse;
import com.example.localai.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/ask")
    public Result<KnowledgeAskResponse> ask(@Valid @RequestBody KnowledgeAskRequest request) {
        return Result.success(knowledgeService.ask(request.getDocumentId(), request.getQuestion()));
    }

    @PostMapping("/ask-global")
    public Result<KnowledgeAskResponse> askGlobal(@Valid @RequestBody GlobalKnowledgeAskRequest request) {
        return Result.success(knowledgeService.askGlobal(request.getSessionId(), request.getQuestion()));
    }
}
