package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 前端直接把图片转成 base64 提交时使用（适合小程序、相机插件等场景）。
 */
public record OcrBase64Request(
        @NotBlank(message = "imageBase64 不能为空")
        String imageBase64) {
}

