package com.lab.ai.pusaman.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author yang.nobel
 * @since 2026-06-13 16:49
 **/
public class JsonUtils {

    private static final ObjectMapper om = new ObjectMapper();

    public static String toJson(Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
