package com.hrm.web.filter;

import com.hrm.security.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class RequestContextFilter extends HttpFilter {

    public static final String REQUEST_ID = "requestId";
    private static final Logger access = LoggerFactory.getLogger("com.hrm.web.access");
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    public static String requestId(HttpServletRequest req) {
        Object id = req.getAttribute(REQUEST_ID);
        return id == null ? MDC.get(REQUEST_ID) : id.toString();
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String incoming = req.getHeader("X-Request-Id");
        String id = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString().substring(0, 13);
        String path = path(req);
        long start = System.nanoTime();
        MDC.put(REQUEST_ID, id);
        req.setAttribute(REQUEST_ID, id);
        resp.setHeader("X-Request-Id", id);
        try {
            chain.doFilter(req, resp);
        } finally {
            if (!isStatic(path)) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                access.info("{} {} -> {} ({} ms)", req.getMethod(), path, resp.getStatus(), ms);
            }
            MDC.clear();
            UserContext.clear();
        }
    }
}
