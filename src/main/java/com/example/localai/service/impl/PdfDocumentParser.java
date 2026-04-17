package com.example.localai.service.impl;

import com.example.localai.service.DocumentParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }

    @Override
    public String parse(Path filePath) throws IOException {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
