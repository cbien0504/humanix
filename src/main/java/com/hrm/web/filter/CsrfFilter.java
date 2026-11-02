package com.hrm.web.filter;

import com.hrm.security.AuthUser;
import com.hrm.security.CsrfTokens;
import com.hrm.web.WebContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF protection with the synchronizer-token pattern (NFR-04).
 * Every state-changing request (POST/PUT/DELETE/PATCH) of a browser session must echo the
 * session token in the {@code X-CSRF-Token} header (AJAX) or the {@code _csrf} form field.
 * Calls authenticated by API key carry no cookies and are therefore exempt.
 * The login page gets a session (and token) before authentication to prevent login CSRF.
 */
public class CsrfFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String path = path(req);
        if (isStatic(path)) {
            chain.doFilter(req, resp);
            return;
        }
        Object principal = req.getAttribute(WebContext.SESSION_USER);
        boolean apiClient = principal instanceof AuthUser u && AuthUser.API_CLIENT.equals(u.getUsername());
        HttpSession session = req.getSession(path.equals("/login"));
        String expected = session == null ? null : CsrfTokens.getOrCreate(session);

        if (!SAFE_METHODS.contains(req.getMethod()) && !apiClient) {
            String actual = req.getHeader(CsrfTokens.HEADER);
            if (actual == null) {
                actual = req.getParameter(CsrfTokens.PARAMETER);
            }
            if (!CsrfTokens.matches(expected, actual)) {
                log.warn("CSRF token mismatch: {} {}", req.getMethod(), path);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Phiên làm việc không hợp lệ hoặc đã hết hạn. Vui lòng tải lại trang và thử lại.");
                return;
            }
        }
        chain.doFilter(req, resp);
    }
}
