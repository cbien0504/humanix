package com.hrm.web.filter;

import com.hrm.security.AccessPolicy;
import com.hrm.security.AuthUser;
import com.hrm.web.WebContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** Role-based URL authorization using {@link AccessPolicy} (first layer of access control). */
public class AuthorizationFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);
    private final AccessPolicy policy;

    public AuthorizationFilter(AccessPolicy policy) {
        this.policy = policy;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        Object principal = req.getAttribute(WebContext.SESSION_USER);
        String path = path(req);
        if (principal instanceof AuthUser user && !policy.isAllowed(user, req.getMethod(), path)) {
            log.warn("Access denied for {} {} {}", user, req.getMethod(), path);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng này.");
            return;
        }
        chain.doFilter(req, resp);
    }
}
