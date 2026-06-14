package com.lab.ai.pusaman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@EnableScheduling
public class PsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsmApplication.class, args);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

}
