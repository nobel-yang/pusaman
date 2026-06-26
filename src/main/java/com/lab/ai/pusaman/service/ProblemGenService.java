package com.lab.ai.pusaman.service;

import com.lab.ai.pusaman.entity.ProblemCache;
import com.lab.ai.pusaman.util.Md5Utils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yang.nobel
 * @since 2026-06-13 18:59
 **/
@Service
public class ProblemGenService {
    private static final Map<Integer, Set<String>> VISITED_PROBLEM_MAP = new ConcurrentHashMap<>();

    public String genNextProblem(int labelId) {
        List<String> problems = ProblemCache.getProblems(labelId);
        Set<String> visitedProblems = VISITED_PROBLEM_MAP.getOrDefault(labelId, new HashSet<>());
        if (visitedProblems.size() == problems.size()) {
            visitedProblems.clear();
        }

        String resultProblem = doGenNextProblem(problems, visitedProblems);
        VISITED_PROBLEM_MAP.put(labelId, visitedProblems);
        return resultProblem;
    }

    private String doGenNextProblem(List<String> problems, Set<String> visitedProblems) {
        Random random = new Random();
        for(;;) {
            int idx = random.nextInt(problems.size());
            String problem = problems.get(idx);
            String problemHash = Md5Utils.encrypt(problem);
            if (!visitedProblems.contains(problemHash)) {
                visitedProblems.add(problemHash);
                return problem;
            }
        }
    }
}
