package com.lab.ai.pusaman.web;

import com.lab.ai.pusaman.annotation.RateLimit;
import com.lab.ai.pusaman.application.ProblemGenApplication;
import com.lab.ai.pusaman.application.QuestionSearchApplication;
import com.lab.ai.pusaman.entity.LabelCache;
import com.lab.ai.pusaman.entity.ProblemCache;
import com.lab.ai.pusaman.entity.ProblemLabelVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yang.nobel
 * @since 2026-06-13 17:49
 **/
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/problem")
public class ProblemController {

    @Resource
    QuestionSearchApplication questionSearchApplication;
    @Resource
    ProblemGenApplication problemGenApplication;

    @GetMapping("/labels")
    @RateLimit(qps = 5.0)
    public ResponseEntity<List<ProblemLabelVo>> labels() {
        return ResponseEntity.ok(
                LabelCache.getAll().entrySet().stream()
                        .map(e -> ProblemLabelVo.builder()
                                .labelId(e.getKey())
                                .labelName(e.getValue())
                                .problemCnt(ProblemCache.getProblemCount(e.getKey()))
                                .build()
                        ).collect(Collectors.toList())
        );
    }

    @GetMapping("/{labelId}/next")
    public ResponseEntity<String> next(@PathVariable Integer labelId) {
        log.info("Next problem label:{}", labelId);
        return ResponseEntity.ok(problemGenApplication.genNextProblem(labelId));
    }

    @RateLimit(qps = 1)
    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestParam(value = "q") String question,
                            @RequestParam(value = "a") String answer) {
        log.info("Question: {}, answer from user:{}", question, answer);
        return questionSearchApplication.searchAnswer(question, answer);
    }
}
