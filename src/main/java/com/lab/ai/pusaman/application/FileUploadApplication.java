package com.lab.ai.pusaman.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.ai.pusaman.entity.JsonFile;
import com.lab.ai.pusaman.entity.LabelCache;
import com.lab.ai.pusaman.entity.MarkdownFile;
import com.lab.ai.pusaman.entity.ProblemCache;
import com.lab.ai.pusaman.entity.ProblemLabel;
import com.lab.ai.pusaman.service.EmbeddingService;
import com.lab.ai.pusaman.service.LabelService;
import com.lab.ai.pusaman.service.MarkdownTitleExtractService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author yang.nobel
 * @since 2026-06-13 18:19
 **/
@Service
public class FileUploadApplication {
    @Resource
    MarkdownTitleExtractService markdownTitleExtractService;
    @Resource
    EmbeddingService embeddingService;
    @Resource
    LabelService labelService;
    @Resource
    ObjectMapper objectMapper;

    public void upload(MarkdownFile file) {
        List<String> titles = markdownTitleExtractService.extractTitle(file);
        int labelId = this.getLabelId(file.getFileName());
        ProblemCache.addProblem(labelId, titles);

        embeddingService.embed(file.getFile());
    }

    private int getLabelId(String fileName) {
        return LabelCache.getAll().entrySet().stream()
                .filter(e -> fileName.toLowerCase().contains(e.getValue().toLowerCase()))
                .findAny()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalArgumentException("暂不支持该类型数据"));
    }

    public void upload(JsonFile file) throws IOException {
        labelService.save(file);
        this.reloadCache(file);
    }

    private void reloadCache(JsonFile file) {
        List<ProblemLabel> labels;
        try {
            labels = objectMapper.readValue(file.getFile().getBytes(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid label JSON: " + e.getMessage(), e);
        }
        LabelCache.reload(labels);
    }
}
