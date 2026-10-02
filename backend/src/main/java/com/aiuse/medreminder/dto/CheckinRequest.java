package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CheckinRequest(
        @NotNull(message = "elderId 不能为空")
        Long elderId,

        @NotNull(message = "planId 不能为空")
        Long planId,

        @NotNull(message = "打卡日期 takeDate 不能为空")
        LocalDate takeDate,

        @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "remindTime 格式应为 HH:mm")
        String remindTime,

        @Pattern(regexp = "^(TAKEN|MISSED)$", message = "status 只能是 TAKEN 或 MISSED")
        String status,

        @Size(max = 200, message = "备注不能超过200个字符")
        String note) {
}

