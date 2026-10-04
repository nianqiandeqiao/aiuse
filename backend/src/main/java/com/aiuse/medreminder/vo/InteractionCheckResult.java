package com.aiuse.medreminder.vo;

import java.util.List;

/**
 * 一次用药冲突检查的结果。
 */
public record InteractionCheckResult(
        Long elderId,
        List<String> drugs,
        int drugCount,
        int conflictCount,
        Integer maxSeverity,
        String maxSeverityLabel,
        List<InteractionConflict> conflicts) {

    public static InteractionCheckResult of(Long elderId, List<String> drugs,
                                            List<InteractionConflict> conflicts) {
        Integer maxSeverity = conflicts.stream()
                .map(InteractionConflict::severity)
                .min(Integer::compareTo)
                .orElse(null);
        return new InteractionCheckResult(
                elderId,
                drugs,
                drugs.size(),
                conflicts.size(),
                maxSeverity,
                maxSeverity == null ? null : InteractionConflict.severityLabel(maxSeverity),
                conflicts);
    }
}

