package com.aiuse.medreminder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 用药提醒与用药安全助手 - 后端入口。
 */
@SpringBootApplication
@MapperScan("com.aiuse.medreminder.mapper")
public class MedReminderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedReminderApplication.class, args);
    }
}
