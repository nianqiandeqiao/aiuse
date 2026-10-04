package com.aiuse.medreminder.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户（家属/护理员账号）。
 */
@Data
public class AppUser {

    private Long id;
    private String username;
    private String displayName;
    private String passwordHash;
    private LocalDateTime createdAt;
}

