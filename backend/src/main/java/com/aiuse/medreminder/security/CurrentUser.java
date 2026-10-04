package com.aiuse.medreminder.security;

/**
 * 当前登录用户（本次请求内有效）。
 */
public record CurrentUser(Long id, String username, String displayName) {
}

