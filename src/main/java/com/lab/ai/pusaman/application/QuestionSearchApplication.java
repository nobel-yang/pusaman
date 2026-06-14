package com.lab.ai.pusaman.application;

import com.lab.ai.pusaman.service.LLMService;
import com.lab.ai.pusaman.service.SearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author yang.nobel
 * @since 2026-06-12 17:13
 **/
@Slf4j
@Service
public class QuestionSearchApplication {

    @Resource
    SearchService searchService;
    @Resource
    LLMService llmService;

    public Flux<String> searchAnswer(String question, String userAnswer) {
        String hitContent = searchService.search(question);
        String prompt = String.format("题目：%s，用户答案：%s。参考资料: %s", question, userAnswer, hitContent);
        return llmService.ask(prompt);
    }
}
