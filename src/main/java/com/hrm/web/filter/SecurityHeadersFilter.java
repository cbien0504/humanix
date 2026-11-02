package com.hrm.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Adds defensive HTTP headers (NFR-03).
 * The Content-Security-Policy only allows scripts from this origin – there is no inline
 * JavaScript in any JSP – so even if an XSS payload slipped through output escaping it could
 * not execute. Dynamic responses are marked {@code no-store} (personal data, salaries).
 */
public class SecurityHeadersFilter extends HttpFilter {

    static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        resp.setHeader("Content-Security-Policy", CSP);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        if (!isStatic(path(req))) {
            resp.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(req, resp);
    }
}
