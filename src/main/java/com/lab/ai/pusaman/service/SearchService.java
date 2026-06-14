package com.lab.ai.pusaman.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yang.nobel
 * @since 2026-06-12 16:44
 **/
@Slf4j
@Service
public class SearchService {

    @Resource
    VectorStore vectorStore;

    public String search(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThreshold(0.07)
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);
        log.info("Vector store search result size:{}", documents.size());
        if (CollectionUtils.isEmpty(documents)) {
            return "";
        }

        return documents.stream()
                .map(doc -> {
                    String source = doc.getMetadata().get("source").toString();
                    return String.format("[%s]\n%s", source, doc.getText());
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
