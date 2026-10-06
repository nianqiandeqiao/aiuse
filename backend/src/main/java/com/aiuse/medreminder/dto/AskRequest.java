package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 语音/文字问药请求。
 */
public record AskRequest(
        @NotNull(message = "elderId 不能为空")
        Long elderId,

        @NotBlank(message = "问题不能为空")
        String question,

        /** 方言标记，如 henan / sichuan / cantonese，用于后端做词表归一 */
        String dialect) {
}

