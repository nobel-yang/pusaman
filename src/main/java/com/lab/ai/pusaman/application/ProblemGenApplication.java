package com.lab.ai.pusaman.application;

import com.lab.ai.pusaman.service.ProblemGenService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author yang.nobel
 * @since 2026-06-13 19:17
 **/
@Service
public class ProblemGenApplication {
    private static final String PATTERN = "^(?:\\*\\*)*\\d+[.\\s、]*";

    @Resource
    ProblemGenService problemGenService;

    public String genNextProblem(int labelId) {
        String problem = problemGenService.genNextProblem(labelId);
        problem = problem.replaceAll("\\*\\*", "");
        return problem.replaceAll(PATTERN, "### ");
    }
}
