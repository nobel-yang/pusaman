package com.lab.ai.pusaman.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author yang.nobel
 * @since 2026-06-13 18:20
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownFile {
    private String fileName;
    private MultipartFile file;
}
