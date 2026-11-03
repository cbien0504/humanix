package com.hrm.web.filter;

import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.security.UserContext;
import com.hrm.web.WebContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.Set;

public class AuthenticationFilter extends HttpFilter {

    public static final String MUST_CHANGE_PASSWORD = "mustChangePassword";
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final Set<String> PUBLIC_PATHS = Set.of("/login", "/health", "/error", "/favicon.ico");

    private final byte[] apiKey;

    public AuthenticationFilter(String apiKey) {
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String path = path(req);
        UserContext.setClientIp(clientIp(req));
        HttpSession session = req.getSession(false);
        AuthUser sessionUser = session == null ? null : (AuthUser) session.getAttribute(WebContext.SESSION_USER);

        if (isStatic(path) || PUBLIC_PATHS.contains(path)) {
            if (sessionUser != null) {
                bind(req, sessionUser);
            }
            chain.doFilter(req, resp);
            return;
        }

        String key = req.getHeader("X-API-Key");
        if (key != null && path.startsWith("/api/")) {
            if (apiKey == null || !MessageDigest.isEqual(apiKey, key.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Rejected API call with invalid API key: {} {}", req.getMethod(), path);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key không hợp lệ.");
                return;
            }
            bind(req, new AuthUser(0, AuthUser.API_CLIENT, null, "Hệ thống tích hợp", EnumSet.of(Role.HR)));
            chain.doFilter(req, resp);
            return;
        }

        if (sessionUser == null) {
            if (WebContext.wantsJson(req)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, req.getRequestedSessionId() != null
                        ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                        : "Chưa xác thực: đăng nhập hoặc gửi header X-API-Key.");
            } else {
                String next = "GET".equals(req.getMethod()) ? path + (req.getQueryString() == null ? "" : "?" + req.getQueryString()) : "/";
                resp.sendRedirect(req.getContextPath() + "/login?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
            }
            return;
        }

        if (Boolean.TRUE.equals(session.getAttribute(MUST_CHANGE_PASSWORD))
                && !path.startsWith("/profile/password") && !path.equals("/logout")) {
            if (WebContext.wantsJson(req)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn cần đổi mật khẩu trước khi tiếp tục.");
            } else {
                resp.sendRedirect(req.getContextPath() + "/profile/password?required=1");
            }
            return;
        }

        bind(req, sessionUser);
        chain.doFilter(req, resp);
    }

    private static void bind(HttpServletRequest req, AuthUser user) {
        req.setAttribute(WebContext.SESSION_USER, user);
        UserContext.set(user);
        MDC.put("user", user.getUsername());
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
