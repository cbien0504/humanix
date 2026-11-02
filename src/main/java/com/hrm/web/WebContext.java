package com.hrm.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.UnauthorizedException;
import com.hrm.exception.ValidationException;
import com.hrm.model.PageRequest;
import com.hrm.security.AuthUser;
import com.hrm.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Per-request facade over the servlet request/response with typed parameter parsing,
 * JSON I/O, view rendering and access to the authenticated user. Parsing failures raise
 * {@link ValidationException}, so handlers never deal with raw {@code NumberFormatException}s.
 */
public final class WebContext {

    /** Session attribute holding the {@link AuthUser}. */
    public static final String SESSION_USER = "authUser";
    private static final String VIEW_PREFIX = "/WEB-INF/views/";

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Map<String, String> pathVariables;

    public WebContext(HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        this.request = request;
        this.response = response;
        this.pathVariables = pathVariables;
    }

    public HttpServletRequest request() {
        return request;
    }

    public HttpServletResponse response() {
        return response;
    }

    // ------------------------------------------------------------ input

    public String pathVar(String name) {
        return pathVariables.get(name);
    }

    /** Numeric path variable; a non-numeric id is reported as "not found". */
    public long pathId(String name) {
        try {
            return Long.parseLong(pathVariables.get(name));
        } catch (NumberFormatException | NullPointerException e) {
            throw new NotFoundException("Đường dẫn không hợp lệ.");
        }
    }

    /** Trimmed request parameter, {@code null} when absent or blank. */
    public String param(String name) {
        String v = request.getParameter(name);
        if (v == null) {
            return null;
        }
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    public Integer intParam(String name) {
        String v = param(name);
        try {
            return v == null ? null : Integer.valueOf(v);
        } catch (NumberFormatException e) {
            throw new ValidationException(name, "Giá trị phải là số nguyên.");
        }
    }

    public Long longParam(String name) {
        String v = param(name);
        try {
            return v == null ? null : Long.valueOf(v);
        } catch (NumberFormatException e) {
            throw new ValidationException(name, "Giá trị phải là số nguyên.");
        }
    }

    public BigDecimal decimalParam(String name) {
        String v = param(name);
        try {
            return v == null ? null : new BigDecimal(v.replace(",", ""));
        } catch (NumberFormatException e) {
            throw new ValidationException(name, "Giá trị phải là số.");
        }
    }

    /** ISO date parameter ({@code yyyy-MM-dd}, the format of HTML date inputs). */
    public LocalDate dateParam(String name) {
        String v = param(name);
        try {
            return v == null ? null : LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new ValidationException(name, "Ngày không hợp lệ (định dạng yyyy-MM-dd).");
        }
    }

    /** Enum parameter by constant name; an unknown value is a 400 listing the allowed ones (not a 500). */
    public <E extends Enum<E>> E enumParam(String name, Class<E> type) {
        String v = param(name);
        if (v == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, v.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(name, "Giá trị không hợp lệ. Cho phép: "
                    + String.join(" | ", Arrays.stream(type.getEnumConstants()).map(Enum::name).toList()) + ".");
        }
    }

    /** Paging/sorting parameters {@code page, size, sort, dir}. */
    public PageRequest pageRequest(int defaultSize) {
        return PageRequest.of(intParam("page"), intParam("size"), param("sort"), param("dir"), defaultSize);
    }

    public Boolean boolParam(String name) {
        String v = param(name);
        return v == null ? null : Boolean.valueOf(v);
    }

    public <T> T readJson(Class<T> type) {
        try (InputStream in = request.getInputStream()) {
            T value = Json.mapper().readValue(in, type);
            if (value == null) {
                throw new ValidationException("body", "Thiếu dữ liệu gửi lên.");
            }
            return value;
        } catch (MismatchedInputException e) {
            String field = jsonPath(e);
            if (field.isEmpty()) {
                throw new ValidationException("body", "Dữ liệu gửi lên không đúng cấu trúc.");
            }
            throw new ValidationException(field, "Trường \"" + field + "\" không đúng định dạng" + expected(e.getTargetType()) + ".");
        } catch (JsonProcessingException e) {
            throw new ValidationException("body", "Dữ liệu gửi lên không đúng định dạng JSON.");
        } catch (IOException e) {
            throw new ValidationException("body", "Không đọc được dữ liệu gửi lên.");
        }
    }

    /** "workDate" for an object body, "[3].workDate" for an array – tells API clients which value to fix. */
    private static String jsonPath(JsonMappingException e) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : e.getPath()) {
            if (ref.getFieldName() != null) {
                sb.append(sb.isEmpty() ? "" : ".").append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    private static String expected(Class<?> type) {
        if (type == null) {
            return "";
        }
        if (type == LocalDate.class) {
            return " (yyyy-MM-dd)";
        }
        if (type == LocalTime.class) {
            return " (HH:mm)";
        }
        if (type == LocalDateTime.class) {
            return " (yyyy-MM-ddTHH:mm:ss)";
        }
        if (type.isEnum()) {
            return " (" + String.join(" | ", Arrays.stream(type.getEnumConstants()).map(String::valueOf).toList()) + ")";
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
            return " (số)";
        }
        return "";
    }

    // ------------------------------------------------------------ output

    public void json(Object body) throws IOException {
        json(HttpServletResponse.SC_OK, body);
    }

    public void json(int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Json.mapper().writeValue(response.getOutputStream(), body);
    }

    public void noContent() {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /** Sets request attributes for the view (chainable). */
    public WebContext attr(String name, Object value) {
        request.setAttribute(name, value);
        return this;
    }

    /** Forwards to {@code /WEB-INF/views/<view>.jsp}. */
    public void render(String view) throws ServletException, IOException {
        request.getRequestDispatcher(VIEW_PREFIX + view + ".jsp").forward(request, response);
    }

    /** Redirects to an application-relative path (context path is prepended). */
    public void redirect(String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /** Streams a file download; the filename is RFC 5987 encoded so Vietnamese names survive. */
    public OutputStream download(String filename, String contentType) throws IOException {
        response.setContentType(contentType);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        return response.getOutputStream();
    }

    /** True for AJAX calls and REST API calls – errors are then answered as JSON. */
    public boolean wantsJson() {
        return wantsJson(request);
    }

    public static boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        return uri.startsWith("/api/")
                || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"));
    }

    // ------------------------------------------------------------ security

    /** Current user or {@code null}. */
    public AuthUser user() {
        Object fromRequest = request.getAttribute(SESSION_USER);
        if (fromRequest instanceof AuthUser u) {
            return u;
        }
        HttpSession session = request.getSession(false);
        return session == null ? null : (AuthUser) session.getAttribute(SESSION_USER);
    }

    public AuthUser requireUser() {
        AuthUser user = user();
        if (user == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập.");
        }
        return user;
    }

    public String clientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
