package com.lab.ai.pusaman.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LabelCache {

    private static volatile Map<Integer, String> CACHE = new ConcurrentHashMap<>();

    public static void reload(List<ProblemLabel> labels) {
        Map<Integer, String> tmp = new ConcurrentHashMap<>();
        labels.forEach(l -> tmp.put(l.getLabelId(), l.getLabelName()));
        CACHE = tmp;
    }

    public static Map<Integer, String> getAll() {
        return Collections.unmodifiableMap(CACHE);
    }
}
