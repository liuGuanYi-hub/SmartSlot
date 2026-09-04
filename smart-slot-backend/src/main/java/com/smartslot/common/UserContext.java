package com.smartslot.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserContext {

    private static final ThreadLocal<CurrentUserInfo> CONTEXT = new ThreadLocal<>();

    public static void set(CurrentUserInfo userInfo) {
        CONTEXT.set(userInfo);
    }

    public static CurrentUserInfo get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        CurrentUserInfo info = get();
        return info != null ? info.getUserId() : null;
    }

    public static String getRole() {
        CurrentUserInfo info = get();
        return info != null ? info.getRole() : null;
    }

    public static boolean isAdmin() {
        return "ROLE_ADMIN".equals(getRole());
    }

    public static void clear() {
        CONTEXT.remove();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentUserInfo {
        private Long userId;
        private String username;
        private String role;
    }
}
