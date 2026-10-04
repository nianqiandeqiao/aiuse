package com.aiuse.medreminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百度智能云 OCR 配置。
 * 密钥只写在 application-local.yml（已被 .gitignore 忽略），不会提交到 Git。
 */
@Data
@Component
@ConfigurationProperties(prefix = "baidu.ocr")
public class BaiduOcrProperties {

    /** 百度智能云应用的 API Key。 */
    private String apiKey;

    /** 百度智能云应用的 Secret Key。 */
    private String secretKey;

    /** 获取 access_token 的地址。 */
    private String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";

    /** 通用文字识别地址。 */
    private String ocrUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
}

