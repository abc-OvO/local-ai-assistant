package com.example.localai.service.impl;

import com.example.localai.config.AppProperties;
import com.example.localai.dto.DeleteDocumentResponse;
import com.example.localai.dto.DocumentDetailResponse;
import com.example.localai.dto.DocumentSummaryResponse;
import com.example.localai.dto.DocumentUploadResponse;
import com.example.localai.exception.BusinessException;
import com.example.localai.exception.DocumentNotFoundException;
import com.example.localai.exception.UnsupportedFileTypeException;
import com.example.localai.mapper.DocumentChunkPersistenceMapper;
import com.example.localai.mapper.DocumentPersistenceMapper;
import com.example.localai.model.DocumentChunk;
import com.example.localai.model.DocumentRecord;
import com.example.localai.service.ChunkingService;
import com.example.localai.service.DocumentParser;
import com.example.localai.service.DocumentService;
import com.example.localai.service.EmbeddingService;
import com.example.localai.service.EmbeddingJsonCodec;
import com.example.localai.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final AppProperties appProperties;

    private final List<DocumentParser> documentParsers;

    private final ChunkingService chunkingService;

    private final EmbeddingService embeddingService;

    private final RetrievalService retrievalService;

    private final DocumentPersistenceMapper documentPersistenceMapper;

    private final DocumentChunkPersistenceMapper documentChunkPersistenceMapper;

    private final EmbeddingJsonCodec embeddingJsonCodec;

    private final ConcurrentMap<String, DocumentRecord> documentStore = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public DocumentUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!StringUtils.hasText(originalFileName)) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String fileType = getFileType(originalFileName);
        DocumentParser parser = findParser(fileType);
        String documentId = UUID.randomUUID().toString();

        try {
            Path uploadDir = Path.of(appProperties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            Path savedPath = uploadDir.resolve(documentId + "." + fileType).normalize();
            file.transferTo(savedPath);

            String content = parser.parse(savedPath);
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(400, "文件未提取到有效文本内容");
            }

            DocumentRecord record = new DocumentRecord(
                    documentId,
                    originalFileName,
                    fileType,
                    content.length(),
                    LocalDateTime.now(),
                    savedPath.toString(),
                    content
            );

            List<DocumentChunk> chunks = buildDocumentChunks(documentId, originalFileName, content);
            documentPersistenceMapper.upsert(record);
            documentChunkPersistenceMapper.deleteByDocumentId(documentId);
            documentChunkPersistenceMapper.insertBatch(chunks);
            retrievalService.saveDocumentChunks(documentId, chunks);

            documentStore.put(documentId, record);
            System.out.println("[DocumentUpload] documentId=" + documentId
                    + ", fileName=" + originalFileName
                    + ", chunkCount=" + chunks.size());
            return toUploadResponse(record);
        } catch (IOException ex) {
            throw new BusinessException(500, "文件保存或解析失败：" + ex.getMessage(), ex);
        }
    }

    @Override
    public List<DocumentSummaryResponse> listDocuments() {
        return documentStore.values()
                .stream()
                .sorted(Comparator.comparing(DocumentRecord::getUploadTime).reversed())
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public DocumentDetailResponse getDocumentDetail(String documentId) {
        DocumentRecord record = getDocument(documentId);
        List<DocumentChunk> chunks = documentChunkPersistenceMapper.findByDocumentId(documentId);
        return new DocumentDetailResponse(
                record.getDocumentId(),
                record.getFileName(),
                record.getFileType(),
                buildPreview(record.getContent(), 500),
                chunks.size(),
                record.getUploadTime(),
                record.getUpdatedAt() == null ? record.getUploadTime() : record.getUpdatedAt(),
                chunks.stream()
                        .map(chunk -> new DocumentDetailResponse.ChunkBrief(
                                chunk.getChunkId(),
                                chunk.getChunkIndex(),
                                buildPreview(chunk.getContent(), 160),
                                embeddingDimension(chunk)
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional
    public DeleteDocumentResponse deleteDocument(String documentId) {
        DocumentRecord record = getDocument(documentId);

        documentChunkPersistenceMapper.deleteByDocumentId(documentId);
        documentPersistenceMapper.deleteByDocumentId(documentId);
        retrievalService.deleteDocumentChunks(documentId);
        documentStore.remove(documentId);

        if (StringUtils.hasText(record.getSavedPath())) {
            try {
                Files.deleteIfExists(Path.of(record.getSavedPath()));
            } catch (IOException ex) {
                System.out.println("[DocumentDelete] upload file delete skipped, documentId=" + documentId
                        + ", path=" + record.getSavedPath()
                        + ", error=" + ex.getMessage());
            }
        }

        System.out.println("[DocumentDelete] documentId=" + documentId + ", deleted=true");
        return new DeleteDocumentResponse(documentId, true);
    }

    @Override
    public DocumentRecord getDocument(String documentId) {
        DocumentRecord record = documentStore.get(documentId);
        if (record == null) {
            throw new DocumentNotFoundException(documentId);
        }
        return record;
    }

    @Override
    public void restorePersistedDocuments(List<DocumentRecord> documents) {
        documentStore.clear();
        for (DocumentRecord document : documents) {
            documentStore.put(document.getDocumentId(), document);
        }
    }

    private DocumentParser findParser(String fileType) {
        return documentParsers.stream()
                .filter(parser -> parser.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException("不支持的文件类型，仅支持 txt、md、pdf"));
    }

    private String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new UnsupportedFileTypeException("文件缺少扩展名，仅支持 txt、md、pdf");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private List<DocumentChunk> buildDocumentChunks(String documentId, String fileName, String content) {
        List<String> chunkContents = chunkingService.split(content);
        if (chunkContents.isEmpty()) {
            throw new BusinessException(400, "文件未生成有效文本块");
        }

        return java.util.stream.IntStream.range(0, chunkContents.size())
                .mapToObj(index -> {
                    String chunkContent = chunkContents.get(index);
                    DocumentChunk chunk = new DocumentChunk(
                            UUID.randomUUID().toString(),
                            documentId,
                            fileName,
                            index,
                            chunkContent,
                            embeddingService.embed(chunkContent),
                            null
                    );
                    chunk.setEmbeddingJson(embeddingJsonCodec.toJson(chunk.getEmbedding()));
                    return chunk;
                })
                .toList();
    }

    private DocumentUploadResponse toUploadResponse(DocumentRecord record) {
        return new DocumentUploadResponse(
                record.getDocumentId(),
                record.getFileName(),
                record.getContentLength(),
                record.getUploadTime()
        );
    }

    private DocumentSummaryResponse toSummaryResponse(DocumentRecord record) {
        return new DocumentSummaryResponse(
                record.getDocumentId(),
                record.getFileName(),
                record.getFileType(),
                record.getContentLength(),
                record.getUploadTime(),
                documentChunkPersistenceMapper.countByDocumentId(record.getDocumentId()),
                record.getUploadTime(),
                record.getUpdatedAt() == null ? record.getUploadTime() : record.getUpdatedAt()
        );
    }

    private String buildPreview(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        int previewLength = Math.min(content.length(), maxLength);
        return content.substring(0, previewLength);
    }

    private Integer embeddingDimension(DocumentChunk chunk) {
        if (chunk.getEmbedding() != null) {
            return chunk.getEmbedding().size();
        }
        if (StringUtils.hasText(chunk.getEmbeddingJson())) {
            return embeddingJsonCodec.fromJson(chunk.getEmbeddingJson()).size();
        }
        return 0;
    }
}
