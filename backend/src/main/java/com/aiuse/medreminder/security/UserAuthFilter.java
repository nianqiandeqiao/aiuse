package com.aiuse.medreminder.security;

import com.aiuse.medreminder.entity.AppUser;
import com.aiuse.medreminder.mapper.AppUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 登录校验过滤器：除登录注册接口、健康检查和前端静态资源外，
 * 其余接口都必须带合法会话，否则返回 401。
 */
@Component
public class UserAuthFilter extends OncePerRequestFilter {

    /** 会话中保存用户 id 的键。 */
    public static final String SESSION_USER_ID = "MED_USER_ID";

    private final AppUserMapper userMapper;

    public UserAuthFilter(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }

            HttpSession session = request.getSession(false);
            Object userId = session == null ? null : session.getAttribute(SESSION_USER_ID);
            if (userId == null) {
                writeUnauthorized(response);
                return;
            }

            AppUser user = userMapper.findById(((Number) userId).longValue()).orElse(null);
            if (user == null) {
                session.invalidate();
                writeUnauthorized(response);
                return;
            }

            CurrentUserHolder.set(new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName()));
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserHolder.clear();
        }
    }

    /**
     * 只有注册、登录、登录状态、健康检查和前端静态外壳不需要登录；
     * /api/auth/me 与 /api/auth/logout 仍需经过本过滤器，才能拿到当前用户。
     */
    private boolean isPublicPath(String path) {
        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/status")
                || path.startsWith("/api/health")
                || path.equals("/")
                || path.equals("/index.html")
                || path.equals("/favicon.ico")
                || path.startsWith("/assets/");
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":401,\"message\":\"未登录或登录已过期，请先登录\",\"data\":null}");
    }
}
