package com.lab.ai.pusaman.service;

import com.lab.ai.pusaman.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}
