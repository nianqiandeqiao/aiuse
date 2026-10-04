package com.aiuse.medreminder.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 访问控制过滤器：配置了口令时，除登录接口和前端静态资源外，其余接口都要求先登录。
 * 未配置口令（本地演示）时该过滤器直接放行，不影响原有流程。
 */
@Component
public class AccessAuthFilter extends OncePerRequestFilter {

    /** 登录成功后写入 session 的标记。 */
    public static final String SESSION_KEY = "MED_ACCESS_GRANTED";

    private final AccessProperties properties;

    public AccessAuthFilter(AccessProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_KEY))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":401,\"message\":\"未登录或登录已过期，请先输入访问口令\",\"data\":null}");
    }

    /**
     * 放行登录接口本身，以及前端静态资源（页面外壳不含任何数据，
     * 真正的数据接口仍需要登录）。
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/")
                || path.equals("/")
                || path.equals("/index.html")
                || path.equals("/favicon.ico")
                || path.startsWith("/assets/");
    }
}

