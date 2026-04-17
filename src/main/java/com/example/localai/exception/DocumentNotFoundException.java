package com.example.localai.exception;

public class DocumentNotFoundException extends BusinessException {

    public DocumentNotFoundException(String documentId) {
        super(404, "文档不存在，documentId：" + documentId);
    }
}
