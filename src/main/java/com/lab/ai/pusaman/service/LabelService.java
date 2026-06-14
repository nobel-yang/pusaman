package com.lab.ai.pusaman.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.ai.pusaman.entity.JsonFile;
import com.lab.ai.pusaman.entity.LabelCache;
import com.lab.ai.pusaman.entity.ProblemLabel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class LabelService {
    private final ObjectMapper objectMapper;
    private final String filePath;

    public LabelService(ObjectMapper objectMapper,
                        @Value("${pusaman.cache.file-path}") String filePath) {
        this.objectMapper = objectMapper;
        this.filePath = filePath + "/label.json";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadFromFile() {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            log.warn("Label file not found at {}, starting with empty labels", filePath);
            return;
        }

        try {
            List<ProblemLabel> labels = objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<>() {}
            );
            LabelCache.reload(labels);
            log.info("Labels loaded from {}, {} entries", filePath, labels.size());
        } catch (IOException e) {
            log.error("Failed to load labels from {}", filePath, e);
            throw new IllegalStateException("Failed to load labels from " + filePath);
        }
    }

    public void save(JsonFile file) throws IOException {
        Path path = Path.of(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getFile().getBytes());
        log.info("Labels saved to {}", filePath);
    }
}
