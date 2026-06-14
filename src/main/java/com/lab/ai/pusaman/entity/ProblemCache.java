package com.lab.ai.pusaman.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yang.nobel
 * @since 2026-06-13 18:33
 **/
public class ProblemCache {

    private static final Map<Integer, List<String>> cache = new ConcurrentHashMap<>();

    public static void addProblem(Integer labelId, String problem) {
        cache.computeIfAbsent(labelId, k -> new ArrayList<>()).add(problem);
    }

    public static void addProblem(Integer labelId, List<String> problems) {
        problems.forEach(p -> ProblemCache.addProblem(labelId, p));
    }

    public static List<String> getProblems(Integer labelId) {
        return cache.getOrDefault(labelId, Collections.emptyList());
    }

    public static int getProblemCount(Integer labelId) {
        return getProblems(labelId).size();
    }

    public static Map<Integer, List<String>> getAll() {
        return Collections.unmodifiableMap(cache);
    }
}
