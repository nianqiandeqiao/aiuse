package com.aiuse.medreminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 访问口令配置。
 *
 * <p>本地演示默认不启用（password 为空）；把系统发布到公网给他人访问时，
 * 必须设置口令，否则任何人拿到网址都能查看和修改老人用药数据。</p>
 *
 * <p>可以用环境变量注入，避免把口令写进配置文件：APP_ACCESS_PASSWORD</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.access")
public class AccessProperties {

    /** 访问口令；为空表示不启用访问控制。 */
    private String password = "";

    /** 登录后会话有效期（分钟），默认 12 小时。 */
    private int sessionMinutes = 720;

    public boolean isEnabled() {
        return StringUtils.hasText(password);
    }
}

