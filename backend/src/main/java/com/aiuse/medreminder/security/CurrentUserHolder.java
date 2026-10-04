package com.aiuse.medreminder.security;

import com.aiuse.medreminder.common.BizException;

/**
 * 当前请求的登录用户上下文（ThreadLocal）。
 * 由 UserAuthFilter 在请求开始时写入、结束时清理，业务层用它来隔离数据归属。
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 取当前用户 id；没有登录直接抛 401 业务异常。 */
    public static long requireUserId() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期，请先登录");
        }
        return user.id();
    }
}

