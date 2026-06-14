package com.lab.ai.pusaman.admin;

import com.lab.ai.pusaman.application.FileUploadApplication;
import com.lab.ai.pusaman.entity.JsonFile;
import com.lab.ai.pusaman.entity.MarkdownFile;
import com.lab.ai.pusaman.entity.ProblemCache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    FileUploadApplication fileUploadApplication;

    @PutMapping("/file/doc")
    public ResponseEntity<String> uploadDoc(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            log.info("File upload request:{}", fileName);
            Objects.requireNonNull(fileName);
            if (!fileName.contains(".md")) {
                throw new IllegalArgumentException("Supported markdown only.");
            }

            fileUploadApplication.upload(
                    MarkdownFile.builder()
                            .fileName(fileName)
                            .file(file)
                            .build()
            );
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Failed to save doc file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/file/label")
    public ResponseEntity<String> uploadLabel(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            log.info("File upload request:{}", fileName);
            Objects.requireNonNull(fileName);
            if (!fileName.contains(".json")) {
                throw new IllegalArgumentException("Supported json only.");
            }

            fileUploadApplication.upload(
                    JsonFile.builder()
                            .fileName(file.getOriginalFilename())
                            .file(file)
                            .build()
            );
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Failed to save label file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/file/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            if (Path.of(fileName).isAbsolute()) {
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            Path base = Path.of("data").toAbsolutePath().normalize();
            Path target = base.resolve(fileName).normalize();

            if (!target.startsWith(base)) {
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            if (!Files.exists(target)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            Files.delete(target);
            log.info("Deleted file: {}", target);
            return ResponseEntity.ok("deleted");
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/problem")
    public ResponseEntity<Map<Integer, List<String>>> getProblem() {
        return ResponseEntity.ok(ProblemCache.getAll());
    }
}
