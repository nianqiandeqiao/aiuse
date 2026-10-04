package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 按老人检查冲突：传老人 id，后端自动取他的全部进行中用药计划。
 */
public record InteractionCheckRequest(
        @NotNull(message = "elderId 不能为空")
        Long elderId) {
}

