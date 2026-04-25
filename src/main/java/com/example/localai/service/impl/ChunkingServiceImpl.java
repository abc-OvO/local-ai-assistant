package com.example.localai.service.impl;

import com.example.localai.config.AppProperties;
import com.example.localai.service.ChunkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private final AppProperties appProperties;

    @Override
    public List<String> split(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        int chunkSize = appProperties.getChunkSize();
        int chunkOverlap = Math.min(appProperties.getChunkOverlap(), chunkSize - 1);
        int step = chunkSize - chunkOverlap;
        List<String> chunks = new ArrayList<>();

        for (int start = 0; start < content.length(); start += step) {
            int end = Math.min(start + chunkSize, content.length());
            String chunk = content.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end >= content.length()) {
                break;
            }
        }
        return chunks;
    }
}
