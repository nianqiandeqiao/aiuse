package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MedicationPlanCreateRequest(
        @NotNull(message = "elderId 不能为空")
        Long elderId,

        @NotBlank(message = "药品名称不能为空")
        @Size(max = 100, message = "药品名称不能超过100个字符")
        String drugName,

        @NotBlank(message = "剂量不能为空")
        @Size(max = 50, message = "剂量不能超过50个字符")
        String dosage,

        @Size(max = 50, message = "服用频次不能超过50个字符")
        String frequency,

        @Size(max = 20, message = "服用时机不能超过20个字符")
        String takeRelation,

        @Size(max = 100, message = "提醒时间不能超过100个字符")
        String remindTimes,

        LocalDate startDate,
        LocalDate endDate) {
}

