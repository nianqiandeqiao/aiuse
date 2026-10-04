package com.aiuse.medreminder.vo;

/**
 * 一条冲突提示：可能来自相互作用规则，也可能是重复用药。
 */
public record InteractionConflict(
        String type,
        String drugA,
        String drugB,
        int severity,
        String severityLabel,
        String title,
        String description,
        String advice,
        String source) {

    public static final String TYPE_INTERACTION = "INTERACTION";
    public static final String TYPE_DUPLICATE = "DUPLICATE";

    public static String severityLabel(int severity) {
        return switch (severity) {
            case 1 -> "严重";
            case 2 -> "警告";
            default -> "注意";
        };
    }
}

