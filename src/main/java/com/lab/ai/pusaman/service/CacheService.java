package com.lab.ai.pusaman.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.ai.pusaman.entity.ProblemCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CacheService {

    private final ObjectMapper objectMapper;
    private final String filePath;

    public CacheService(ObjectMapper objectMapper,
                        @Value("${pusaman.cache.file-path}") String filePath) {
        this.objectMapper = objectMapper;
        this.filePath = filePath + "/problem-cache.json";
    }

    @Scheduled(fixedRate = 60 * 1000)
    public void persistToFile() {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Map<Integer, List<String>> snapshot = ProblemCache.getAll().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> List.copyOf(e.getValue())));
            if (snapshot.isEmpty()) {
                return;
            }
            objectMapper.writeValue(path.toFile(), snapshot);
            log.debug("ProblemCache persisted to {}, {} labels", filePath, snapshot.size());
        } catch (IOException e) {
            log.error("Failed to persist ProblemCache to {}", filePath, e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadFromFile() {
        if (!ProblemCache.getAll().isEmpty()) {
            log.warn("Cache already has data, skipping load from file");
            return;
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            log.warn("Cache file not found at {}, starting with empty cache", filePath);
            return;
        }

        try {
            Map<Integer, List<String>> data = objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<>() {}
            );
            data.forEach(ProblemCache::addProblem);
            log.info("ProblemCache loaded from {}, {} labels", filePath, data.size());
        } catch (IOException e) {
            log.error("Failed to load ProblemCache from {}, starting with empty cache", filePath, e);
        }
    }
}
