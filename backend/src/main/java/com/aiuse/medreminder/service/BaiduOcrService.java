package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.config.BaiduOcrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * 百度智能云"通用文字识别"封装：负责拿 access_token（带缓存）和调用 OCR 接口。
 */
@Service
public class BaiduOcrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduOcrService.class);

    /** 提前 5 分钟判定 token 过期，避免边界失效。 */
    private static final long TOKEN_SAFETY_WINDOW_SECONDS = 300L;

    private final BaiduOcrProperties properties;
    private final RestClient restClient;

    private volatile String cachedToken;
    private volatile long tokenExpiresAtMillis;

    public BaiduOcrService(BaiduOcrProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getApiKey()) && StringUtils.hasText(properties.getSecretKey());
    }

    /** 供语音识别等其他百度服务复用同一个 access_token（避免重复申请）。 */
    public String accessTokenForOtherService() {
        return accessToken();
    }

    /** 识别图片中的全部文字，按行返回。 */
    public List<String> recognizeLines(byte[] imageBytes) {
        if (!isConfigured()) {
            throw new BizException(500,
                    "未配置百度 OCR 密钥，请在 application-local.yml 中填写 baidu.ocr.api-key 与 baidu.ocr.secret-key");
        }
        String token = accessToken();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("image", Base64.getEncoder().encodeToString(imageBytes));
        form.add("detect_direction", "true");

        BaiduOcrResponse response;
        try {
            response = restClient.post()
                    .uri(properties.getOcrUrl() + "?access_token={token}", token)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(BaiduOcrResponse.class);
        } catch (Exception e) {
            log.error("调用百度 OCR 失败", e);
            throw new BizException(500, "调用百度 OCR 失败: " + e.getMessage());
        }
        if (response == null) {
            throw new BizException(500, "百度 OCR 返回内容为空");
        }
        if (response.error_code() != null) {
            throw new BizException(500,
                    "百度 OCR 返回错误 " + response.error_code() + ": " + response.error_msg());
        }
        if (response.words_result() == null) {
            return List.of();
        }
        return response.words_result().stream()
                .map(BaiduOcrResponse.Word::words)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 获取 access_token，内存缓存，快过期时自动刷新。 */
    private String accessToken() {
        long now = System.currentTimeMillis();
        String token = cachedToken;
        if (token != null && now < tokenExpiresAtMillis) {
            return token;
        }
        synchronized (this) {
            if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAtMillis) {
                return cachedToken;
            }
            BaiduTokenResponse response;
            try {
                response = restClient.post()
                        .uri(properties.getTokenUrl()
                                        + "?grant_type=client_credentials&client_id={ak}&client_secret={sk}",
                                properties.getApiKey(), properties.getSecretKey())
                        .retrieve()
                        .body(BaiduTokenResponse.class);
            } catch (Exception e) {
                throw new BizException(500, "获取百度 access_token 失败: " + e.getMessage());
            }
            if (response == null || !StringUtils.hasText(response.access_token())) {
                String detail = response == null ? "返回为空" : response.error_description();
                throw new BizException(500, "获取百度 access_token 失败: " + detail);
            }
            cachedToken = response.access_token();
            long expiresIn = response.expires_in() == null ? 2592000L : response.expires_in();
            tokenExpiresAtMillis = System.currentTimeMillis()
                    + Math.max(60L, expiresIn - TOKEN_SAFETY_WINDOW_SECONDS) * 1000L;
            log.info("百度 access_token 获取成功，{} 秒后过期", expiresIn);
            return cachedToken;
        }
    }

    /** 百度 token 接口返回结构（字段名与百度保持一致，避免额外配置）。 */
    public record BaiduTokenResponse(
            String access_token,
            Integer expires_in,
            String error,
            String error_description) {
    }

    /** 百度通用文字识别返回结构。 */
    public record BaiduOcrResponse(
            List<Word> words_result,
            Integer words_result_num,
            String error_code,
            String error_msg) {

        public record Word(String words) {
        }
    }
}
