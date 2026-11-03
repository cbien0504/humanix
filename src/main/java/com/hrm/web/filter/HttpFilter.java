package com.hrm.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class HttpFilter implements Filter {

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    protected abstract void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException;

    protected static String path(HttpServletRequest req) {
        return req.getRequestURI().substring(req.getContextPath().length());
    }

    protected static boolean isStatic(String path) {
        return path.startsWith("/assets/") || path.startsWith("/webjars/") || path.equals("/favicon.ico");
    }
}
