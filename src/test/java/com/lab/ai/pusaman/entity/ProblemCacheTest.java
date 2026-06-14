package com.lab.ai.pusaman.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProblemCacheTest {

    @BeforeEach
    void clearCache() throws Exception {
        var field = ProblemCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        ((java.util.Map<?, ?>) field.get(null)).clear();
    }

    @Test
    void getAll_returnsAllEntries() {
        ProblemCache.addProblem(1, "MySQL问题1");
        ProblemCache.addProblem(1, "MySQL问题2");
        ProblemCache.addProblem(2, "Redis问题1");

        Map<Integer, List<String>> all = ProblemCache.getAll();

        assertEquals(2, all.size());
        assertEquals(List.of("MySQL问题1", "MySQL问题2"), all.get(1));
        assertEquals(List.of("Redis问题1"), all.get(2));
    }

    @Test
    void getAll_returnsUnmodifiableView() {
        ProblemCache.addProblem(1, "问题1");
        Map<Integer, List<String>> all = ProblemCache.getAll();

        assertThrows(UnsupportedOperationException.class, () -> all.put(99, List.of("x")));
    }

    @Test
    void getAll_emptyWhenCacheEmpty() {
        assertTrue(ProblemCache.getAll().isEmpty());
    }
}
