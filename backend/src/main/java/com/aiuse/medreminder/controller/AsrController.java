package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.AsrRequest;
import com.aiuse.medreminder.service.BaiduAsrService;
import com.aiuse.medreminder.vo.AsrResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

/**
 * 语音识别：浏览器录音 → 后端转百度识别 → 返回文字。
 */
@RestController
@RequestMapping("/api/asr")
public class AsrController {

    private final BaiduAsrService asrService;

    public AsrController(BaiduAsrService asrService) {
        this.asrService = asrService;
    }

    @PostMapping("/recognize")
    public ApiResponse<AsrResult> recognize(@Valid @RequestBody AsrRequest request) {
        String base64 = request.audioBase64();
        int commaIndex = base64.indexOf(',');
        if (base64.startsWith("data:") && commaIndex > 0) {
            base64 = base64.substring(commaIndex + 1);
        }
        byte[] audio;
        try {
            audio = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw BizException.badRequest("音频数据格式不正确");
        }
        return ApiResponse.ok(new AsrResult(asrService.recognizeWav(audio), "BAIDU"));
    }
}

