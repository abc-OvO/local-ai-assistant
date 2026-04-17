package com.example.localai.controller;

import com.example.localai.common.Result;
import com.example.localai.dto.DocumentSummaryResponse;
import com.example.localai.dto.DocumentUploadResponse;
import com.example.localai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public Result<DocumentUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(documentService.upload(file));
    }

    @GetMapping
    public Result<List<DocumentSummaryResponse>> listDocuments() {
        return Result.success(documentService.listDocuments());
    }
}
