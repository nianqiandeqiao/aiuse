package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 药盒说明书解析请求：把 OCR 识别出来的文字交给系统分析。
 */
public record MedicineAnalysisRequest(
        @NotNull(message = "elderId 不能为空")
        Long elderId,

        @NotBlank(message = "说明书文字不能为空")
        String text) {
}

