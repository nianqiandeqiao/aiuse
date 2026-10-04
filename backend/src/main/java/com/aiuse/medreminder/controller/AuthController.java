package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.dto.UserLoginRequest;
import com.aiuse.medreminder.dto.UserRegisterRequest;
import com.aiuse.medreminder.entity.AppUser;
import com.aiuse.medreminder.security.CurrentUser;
import com.aiuse.medreminder.security.CurrentUserHolder;
import com.aiuse.medreminder.security.UserAuthFilter;
import com.aiuse.medreminder.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 账号接口：注册、登录、退出、查询当前用户。
 * 每个账号只能看到并修改自己的老人档案、用药计划与打卡记录。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Value("${app.auth.allow-register:true}")
    private boolean allowRegister;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody UserRegisterRequest body,
                                                     HttpServletRequest request) {
        if (!allowRegister) {
            return ApiResponse.error(403, "系统已关闭自助注册，请联系管理员开通账号");
        }
        AppUser user = userService.register(body.username(), body.password(), body.displayName());
        bindSession(request, user);
        return ApiResponse.ok(userInfo(user));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody UserLoginRequest body,
                                                  HttpServletRequest request) {
        AppUser user = userService.login(body.username(), body.password());
        bindSession(request, user);
        return ApiResponse.ok(userInfo(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.ok();
    }

    /** 当前登录用户信息；未登录返回 code=401。 */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        CurrentUser current = CurrentUserHolder.get();
        if (current == null) {
            return ApiResponse.error(401, "未登录或登录已过期，请先登录");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", current.id());
        data.put("username", current.username());
        data.put("displayName", current.displayName());
        return ApiResponse.ok(data);
    }

    /** 前端启动时调用：是否需要登录（恒为 true）以及当前登录状态。 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null && session.getAttribute(UserAuthFilter.SESSION_USER_ID) != null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("required", true);
        data.put("loggedIn", loggedIn);
        data.put("allowRegister", allowRegister);
        return ApiResponse.ok(data);
    }

    private void bindSession(HttpServletRequest request, AppUser user) {
        HttpSession session = request.getSession(true);
        session.setAttribute(UserAuthFilter.SESSION_USER_ID, user.getId());
        session.setMaxInactiveInterval(7 * 24 * 3600);
    }

    private Map<String, Object> userInfo(AppUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("displayName", user.getDisplayName());
        return data;
    }
}

