package com.aiuse.medreminder.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 药物相互作用规则实体，字段与 interaction_rule 表一一对应。
 */
@Data
public class InteractionRule {

    private Long id;
    private String drugA;
    private String drugB;
    /** 1 严重，2 警告，3 注意。 */
    private Integer severity;
    private String title;
    private String description;
    private String advice;
    private String source;
    private Boolean enabled;
    private LocalDateTime createdAt;

    /** 判断这条规则是否命中给定的两个药名（不分先后顺序）。 */
    public boolean matches(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return (first.equals(drugA) && second.equals(drugB))
                || (first.equals(drugB) && second.equals(drugA));
    }
}

