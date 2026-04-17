package com.example.localai.service;

import com.example.localai.dto.DocumentSummaryResponse;
import com.example.localai.dto.DocumentUploadResponse;
import com.example.localai.model.DocumentRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentUploadResponse upload(MultipartFile file);

    List<DocumentSummaryResponse> listDocuments();

    DocumentRecord getDocument(String documentId);
}
