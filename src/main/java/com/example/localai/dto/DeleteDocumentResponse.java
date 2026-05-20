package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteDocumentResponse {

    private String documentId;

    private Boolean deleted;
}
