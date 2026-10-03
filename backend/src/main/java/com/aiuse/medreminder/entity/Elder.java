package com.aiuse.medreminder.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 老人档案实体，字段与 elder 表一一对应。
 */
@Data
public class Elder {

    private Long id;
    private String name;
    private Integer age;
    private String gender;
    private String phone;
    private String chronicDisease;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
}

