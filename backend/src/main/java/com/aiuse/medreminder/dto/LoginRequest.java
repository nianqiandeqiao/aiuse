package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "访问口令不能为空")
        String password) {
}

