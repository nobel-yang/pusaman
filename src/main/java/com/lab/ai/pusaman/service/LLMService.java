package com.lab.ai.pusaman.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author yang.nobel
 * @since 2026-06-12 16:53
 **/
@Slf4j
@Service
public class LLMService {

    @Resource
    ChatClient chatClient;

    public Flux<String> ask(String question) {
        log.debug("Question to LLM: {}", question);
        return chatClient.prompt(question)
                .stream()
                .content();
    }
}
