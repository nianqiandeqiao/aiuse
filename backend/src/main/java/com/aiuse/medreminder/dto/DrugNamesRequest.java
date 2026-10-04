package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 直接检查一组药名（医生开药前预检、或者前端添加药品时即时提示）。
 */
public record DrugNamesRequest(
        @NotEmpty(message = "drugNames 不能为空")
        List<String> drugNames) {
}

