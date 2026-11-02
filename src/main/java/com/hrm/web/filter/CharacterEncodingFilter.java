package com.hrm.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Decodes request parameters as UTF-8 (Vietnamese text). Responses declare their own charset
 * (JSP page directive, application/json;charset=UTF-8, text/csv;charset=UTF-8) so binary
 * downloads such as .xlsx are not given a meaningless charset parameter.
 */
public class CharacterEncodingFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        chain.doFilter(req, resp);
    }
}
