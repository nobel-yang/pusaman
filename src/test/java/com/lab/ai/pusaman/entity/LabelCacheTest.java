package com.lab.ai.pusaman.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LabelCacheTest {

    @BeforeEach
    void clearCache() throws Exception {
        var field = LabelCache.class.getDeclaredField("CACHE");
        field.setAccessible(true);
        field.set(null, new java.util.concurrent.ConcurrentHashMap<>());
    }

    @Test
    void reload_populatesCache() {
        List<ProblemLabel> labels = List.of(
                ProblemLabel.builder().labelId(1).labelName("MySQL").build(),
                ProblemLabel.builder().labelId(2).labelName("Redis").build()
        );

        LabelCache.reload(labels);

        Map<Integer, String> all = LabelCache.getAll();
        assertEquals(2, all.size());
        assertEquals("MySQL", all.get(1));
        assertEquals("Redis", all.get(2));
    }

    @Test
    void reload_replacesExistingData() {
        LabelCache.reload(List.of(
                ProblemLabel.builder().labelId(1).labelName("MySQL").build()
        ));
        LabelCache.reload(List.of(
                ProblemLabel.builder().labelId(2).labelName("Redis").build()
        ));

        Map<Integer, String> all = LabelCache.getAll();
        assertEquals(1, all.size());
        assertNull(all.get(1));
        assertEquals("Redis", all.get(2));
    }

    @Test
    void getAll_returnsUnmodifiableMap() {
        LabelCache.reload(List.of(
                ProblemLabel.builder().labelId(1).labelName("MySQL").build()
        ));
        Map<Integer, String> all = LabelCache.getAll();

        assertThrows(UnsupportedOperationException.class, () -> all.put(99, "test"));
    }

    @Test
    void getAll_emptyWhenCacheEmpty() {
        assertTrue(LabelCache.getAll().isEmpty());
    }
}
