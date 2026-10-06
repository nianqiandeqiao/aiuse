package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 语音识别请求：前端把录到的音频（16k 单声道 WAV）转成 base64 提交。
 */
public record AsrRequest(
        @NotBlank(message = "audioBase64 不能为空")
        String audioBase64) {
}

