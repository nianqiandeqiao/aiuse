package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.config.AccessAuthFilter;
import com.aiuse.medreminder.config.AccessProperties;
import com.aiuse.medreminder.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 访问口令登录：用于系统发布到公网时的最简访问控制。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccessProperties properties;

    public AuthController(AccessProperties properties) {
        this.properties = properties;
    }

    /** 前端启动时查询：是否需要登录、当前是否已登录。 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest request) {
        boolean required = properties.isEnabled();
        boolean loggedIn = !required || isLoggedIn(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("required", required);
        data.put("loggedIn", loggedIn);
        return ApiResponse.ok(data);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest body,
                                                  HttpServletRequest request) {
        if (!properties.isEnabled()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("required", false);
            data.put("loggedIn", true);
            return ApiResponse.ok(data);
        }
        if (!constantTimeEquals(body.password(), properties.getPassword())) {
            throw BizException.badRequest("访问口令不正确");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(AccessAuthFilter.SESSION_KEY, Boolean.TRUE);
        session.setMaxInactiveInterval(properties.getSessionMinutes() * 60);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("required", true);
        data.put("loggedIn", true);
        return ApiResponse.ok(data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.ok();
    }

    private boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(AccessAuthFilter.SESSION_KEY));
    }

    /** 定长比较，避免通过响应时间推测口令。 */
    private static boolean constantTimeEquals(String raw, String expected) {
        if (raw == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                raw.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}

