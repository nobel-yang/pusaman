package com.lab.ai.pusaman.service;

import com.lab.ai.pusaman.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yang.nobel
 * @since 2026-06-12 15:20
 **/
@Slf4j
@Service
public class EmbeddingService {
    String filePath;

    @Resource
    VectorStore vectorStore;
    @Resource
    QaDocumentTransformer qaDocumentTransformer;

    public EmbeddingService(@Value("${pusaman.cache.file-path}") String filePath) {
        this.filePath = filePath + "/vector_store.json";
    }

    public void embed(MultipartFile file) {
//        DocumentReader documentReader = new MarkdownDocumentReader(file.getResource(),
//                MarkdownDocumentReaderConfig.defaultConfig());
        DocumentReader documentReader = new TextReader(file.getResource());
        List<Document> documents = documentReader.read();
        List<Document> splitDocs = qaDocumentTransformer.apply(documents);
        log.info("Document size:{}, data:{}", documents.size(), JsonUtils.toJson(documents));
        vectorStore.add(splitDocs);
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

    @Component
    public static class QaDocumentTransformer implements DocumentTransformer {
        // 正则表达式：匹配 ##### 数字. **中文** 换行/空格 英文答案
        private static final Pattern pattern = Pattern.compile("#####\\s*\\d+\\.\\s*\\*\\*(.*?)\\*\\*\\s*\\n\\s*(.*)");

        @Override
        public List<Document> apply(List<Document> documents) {
            List<Document> processedDocuments = new ArrayList<>();

            for (Document doc : documents) {
                String content = doc.getText();

                // 首先按照 "##### 数字." 的边界将大文档切分成独立字符串
                // 使用正向预查 (?=...) 保证切分时不丢失边界特征
                String[] rawChunks = content.split("(?=\\n#####\\s*\\d+\\.)");

                for (String rawChunk : rawChunks) {
                    Matcher matcher = pattern.matcher(rawChunk.strip());

                    if (matcher.find()) {
                        String chineseQuestion = matcher.group(1).trim();
                        String englishAnswer = matcher.group(2).trim();

                        // 执行格式规范化，显式指定语义边界
                        String normalizedContent = String.format(
                                "中文问题: %s\n英文答案: %s",
                                chineseQuestion,
                                englishAnswer
                        );

                        // 提取元数据（可选，方便后续在数据库中按题号或语言过滤）
                        Map<String, Object> metadata = doc.getMetadata();
                        metadata.put("type", "qa");

                        processedDocuments.add(new Document(normalizedContent, metadata));
                    }
                }
            }
            return processedDocuments;
        }
    }
}
