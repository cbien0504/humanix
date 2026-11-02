package com.hrm.security;

/**
 * Holds the principal of the request being processed on the current thread.
 * Set by the authentication filter and cleared when the request completes, it lets
 * infrastructure code (audit log, SESSION_CONTEXT for triggers) know the acting user without
 * passing it through every method signature.
 */
public final class UserContext {

    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser get() {
        return CURRENT.get();
    }

    public static String currentUsername() {
        AuthUser user = CURRENT.get();
        return user == null ? null : user.getUsername();
    }

    public static void setClientIp(String ip) {
        CLIENT_IP.set(ip);
    }

    public static String clientIp() {
        return CLIENT_IP.get();
    }

    public static void clear() {
        CURRENT.remove();
        CLIENT_IP.remove();
    }
}
