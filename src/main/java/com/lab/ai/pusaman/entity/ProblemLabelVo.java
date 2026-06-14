package com.lab.ai.pusaman.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yang.nobel
 * @since 2026-06-13 17:54
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemLabelVo {
    Integer labelId;
    String labelName;
    Integer problemCnt;
}
