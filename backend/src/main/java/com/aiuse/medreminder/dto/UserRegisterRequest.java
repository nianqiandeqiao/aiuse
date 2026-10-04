package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度需在 3-20 之间")
        @Pattern(regexp = "^[A-Za-z0-9_\\u4e00-\\u9fa5]+$", message = "用户名只能包含字母、数字、下划线或中文")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
        String password,

        @Size(max = 20, message = "显示名称过长")
        String displayName) {
}

