package com.aiuse.medreminder.vo;

import java.util.List;

/**
 * 问药回答。source 说明答案来自大模型还是内置规则，references 是依据（说明书/规则库）。
 */
public record AskAnswer(
        String answer,
        String source,
        List<String> references,
        List<InteractionConflict> conflicts,
        String spokenText) {

    public static final String SOURCE_LLM = "LLM";
    public static final String SOURCE_RULE = "RULE";
}

