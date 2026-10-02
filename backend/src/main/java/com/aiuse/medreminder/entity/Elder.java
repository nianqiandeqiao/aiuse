package com.aiuse.medreminder.entity;

import java.time.LocalDateTime;

/**
 * 老人档案。
 */
public record Elder(
        Long id,
        String name,
        Integer age,
        String gender,
        String phone,
        String chronicDisease,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean deleted) {
}

