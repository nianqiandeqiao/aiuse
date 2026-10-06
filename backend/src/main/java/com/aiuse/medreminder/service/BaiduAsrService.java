package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.config.BaiduOcrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 百度短语音识别（ASR）封装。
 *
 * <p>为什么要走后端：浏览器自带的语音识别依赖谷歌服务，国内网络不可用；
 * 改成"浏览器录音 → 后端调用百度语音识别"，在国内才能稳定工作。</p>
 *
 * <p>注意：需要在百度智能云控制台为本应用开通"短语音识别"，
 * 未开通时会返回明确提示，前端会告诉用户该怎么办。</p>
 */
@Service
public class BaiduAsrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduAsrService.class);

    /** 百度短语音识别接口。 */
    private static final String ASR_URL = "https://vop.baidu.com/server_api";

    private final BaiduOcrProperties properties;
    private final BaiduOcrService ocrService;
    private final RestClient restClient = RestClient.create();

    public BaiduAsrService(BaiduOcrProperties properties, BaiduOcrService ocrService) {
        this.properties = properties;
        this.ocrService = ocrService;
    }

    public boolean isConfigured() {
        return ocrService.isConfigured();
    }

    /** 识别 16k 单声道 WAV 音频。 */
    public String recognizeWav(byte[] wavBytes) {
        if (!isConfigured()) {
            throw new BizException(500, "未配置百度密钥，无法使用语音识别");
        }
        if (wavBytes == null || wavBytes.length < 100) {
            throw BizException.badRequest("录音太短，请按住说完一句话再松开");
        }
        String token = ocrService.accessTokenForOtherService();
        Map<String, Object> body = Map.of(
                "format", "wav",
                "rate", 16000,
                "channel", 1,
                "cuid", "med-reminder",
                "token", token,
                "speech", Base64.getEncoder().encodeToString(wavBytes),
                "len", wavBytes.length
        );

        AsrResponse response;
        try {
            response = restClient.post()
                    .uri(ASR_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(AsrResponse.class);
        } catch (Exception e) {
            log.error("调用百度语音识别失败", e);
            throw new BizException(500, "调用语音识别失败: " + e.getMessage());
        }
        if (response == null) {
            throw new BizException(500, "语音识别返回为空");
        }
        if (response.err_no() != null && response.err_no() != 0) {
            // 3302 = 无权限，通常是没有在控制台开通"短语音识别"
            if (response.err_no() == 3302 || (response.err_msg() != null
                    && response.err_msg().contains("permission"))) {
                throw new BizException(500,
                        "百度语音识别还没有开通：请到百度智能云控制台 → 语音技术 → 短语音识别，领取免费额度并勾选到应用，然后重启服务");
            }
            if (response.err_no() == 3301) {
                throw BizException.badRequest("没有听清，请靠近麦克风再说一遍");
            }
            throw new BizException(500, "语音识别失败(" + response.err_no() + "): " + response.err_msg());
        }
        List<String> results = response.result();
        if (results == null || results.isEmpty() || !StringUtils.hasText(results.get(0))) {
            throw BizException.badRequest("没有听清，请再说一遍");
        }
        return results.get(0).trim();
    }

    /** 百度语音识别返回结构。 */
    public record AsrResponse(Integer err_no, String err_msg, String sn, List<String> result) {
    }
}

