package com.aiuse.medreminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 大模型配置（OpenAI 兼容协议）。
 *
 * <p>支持任何 OpenAI 兼容服务：百度千帆 v2、DeepSeek、通义千问、Moonshot、OpenAI 等。
 * 不配置 api-key 时，系统自动使用内置规则引擎回答（药品词典 + 相互作用规则库），
 * 保证没有外部依赖也能演示，且回答有据可查。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    /** 是否启用大模型；没填 api-key 时即使为 true 也会走规则兜底。 */
    private boolean enabled = false;

    /** OpenAI 兼容的 base-url，例如 https://qianfan.baidubce.com/v2 。 */
    private String baseUrl = "https://qianfan.baidubce.com/v2";

    private String apiKey = "";

    private String model = "ernie-4.0-8k";

    private int timeoutSeconds = 40;

    public boolean isUsable() {
        return enabled && StringUtils.hasText(apiKey) && StringUtils.hasText(baseUrl);
    }
}

