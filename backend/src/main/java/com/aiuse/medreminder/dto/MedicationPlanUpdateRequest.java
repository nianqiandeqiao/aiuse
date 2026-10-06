package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 修改用药计划的请求体。elderId 不允许修改，所以这里不包含该字段。
 */
public record MedicationPlanUpdateRequest(
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

        @Pattern(regexp = "^\\s*$|^(\\d{2}:\\d{2})(,\\d{2}:\\d{2})*$",
                message = "remindTimes 格式应为 HH:mm 或 HH:mm,HH:mm")
        String remindTimes,

        LocalDate startDate,
        LocalDate endDate,

        LocalDate expireDate,
        Integer stockQuantity,

        @Size(max = 10, message = "单位不能超过10个字符")
        String stockUnit) {

    /** 兼容旧调用的构造器：不带有效期与剩余量。 */
    public MedicationPlanUpdateRequest(String drugName, String dosage, String frequency,
                                       String takeRelation, String remindTimes, LocalDate startDate,
                                       LocalDate endDate) {
        this(drugName, dosage, frequency, takeRelation, remindTimes, startDate, endDate,
                null, null, null);
    }
}
