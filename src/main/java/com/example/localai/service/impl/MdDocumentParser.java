package com.example.localai.service.impl;

import com.example.localai.service.DocumentParser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class MdDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "md".equalsIgnoreCase(fileType);
    }

    @Override
    public String parse(Path filePath) throws IOException {
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }
}
