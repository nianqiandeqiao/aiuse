package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        @Pattern(regexp = "^\\s*$|^(\\d{2}:\\d{2})(,\\d{2}:\\d{2})*$",
                message = "remindTimes 格式应为 HH:mm 或 HH:mm,HH:mm")
        String remindTimes,

        LocalDate startDate,
        LocalDate endDate,

        /** 药品有效期，用于临期提醒 */
        LocalDate expireDate,

        /** 剩余量，用于缺药提醒 */
        Integer stockQuantity,

        @Size(max = 10, message = "单位不能超过10个字符")
        String stockUnit) {

    /** 兼容旧调用的构造器：不带有效期与剩余量。 */
    public MedicationPlanCreateRequest(Long elderId, String drugName, String dosage, String frequency,
                                       String takeRelation, String remindTimes, LocalDate startDate,
                                       LocalDate endDate) {
        this(elderId, drugName, dosage, frequency, takeRelation, remindTimes, startDate, endDate,
                null, null, null);
    }
}
