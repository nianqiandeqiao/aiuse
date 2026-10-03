package com.aiuse.medreminder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 冒烟测试：Spring 容器能正常启动（会真实连接本机 MySQL）。
 */
@SpringBootTest
@DisplayName("应用上下文启动测试")
class MedReminderApplicationTests {

    @Test
    @DisplayName("Spring 容器加载成功")
    void contextLoads() {
    }
}

