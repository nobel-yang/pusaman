package com.lab.ai.pusaman.service;

import com.lab.ai.pusaman.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author yang.nobel
 * @since 2026-06-12 15:20
 **/
@Slf4j
@Service
public class EmbeddingService {
    @Resource
    VectorStore vectorStore;
    String filePath;

    public EmbeddingService(@Value("${pusaman.cache.file-path}") String filePath) {
        this.filePath = filePath + "/vector_store.json";
    }

    public void embed(MultipartFile file) {
        DocumentReader documentReader = new MarkdownDocumentReader(file.getResource(),
                MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(true)
                        .withIncludeBlockquote(true)
                        .withAdditionalMetadata("source", file.getOriginalFilename())
                        .build());
        List<Document> documents = documentReader.read();
//        TokenTextSplitter splitter = TokenTextSplitter.builder()
//                .withChunkSize(600)
//                .withMinChunkSizeChars(300)
//                .withKeepSeparator(true)
//                .build();
//
//        List<Document> splitDocs = splitter.apply(documents);
        log.info("Document size:{}, data:{}", documents.size(), JsonUtils.toJson(documents));
        vectorStore.add(documents);
    }

    @Scheduled(fixedRate = 300 * 1000)
    @PreDestroy
    public void persistToFile() {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            ((SimpleVectorStore) vectorStore).save(path.toFile());
            log.debug("Vector store saved to {}", path);
        } catch (IOException e) {
            log.error("Error persisting embedding", e);
        }
    }

    // 在应用启动时自动加载已持久化的向量数据
    @PostConstruct
    public void loadStore() {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            log.warn("Vector store file not found: {}", path);
            return;
        }

        ((SimpleVectorStore) vectorStore).load(path.toFile());
        log.info("Vector store loaded from {}", path);
    }
}
