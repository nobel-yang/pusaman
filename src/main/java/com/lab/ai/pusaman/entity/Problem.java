package com.lab.ai.pusaman.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yang.nobel
 * @since 2026-06-13 18:34
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    private String labelId;
    private String content;
}
