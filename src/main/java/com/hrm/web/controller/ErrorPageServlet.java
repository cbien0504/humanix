package com.hrm.web.controller;

import com.hrm.util.Json;
import com.hrm.web.ErrorResponse;
import com.hrm.web.filter.RequestContextFilter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class ErrorPageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ErrorPageServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer status = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable error = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String uri = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String requestId = RequestContextFilter.requestId(req);
        int code = status == null ? 500 : status;

        if (error != null) {
            MDC.put(RequestContextFilter.REQUEST_ID, String.valueOf(requestId));
            log.error("Unhandled exception for {}", uri, error);
            MDC.clear();
        }

        String filterMessage = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String message = switch (code) {
            case 401, 403 -> filterMessage != null && !filterMessage.isBlank() ? filterMessage
                    : (code == 401 ? "Vui lòng đăng nhập." : "Bạn không có quyền truy cập chức năng này.");
            case 404 -> "Không tìm thấy trang hoặc tài nguyên yêu cầu.";
            case 405 -> "Phương thức không được hỗ trợ cho đường dẫn này.";
            default -> code >= 500 ? "Đã xảy ra lỗi hệ thống. Vui lòng thử lại hoặc liên hệ bộ phận IT kèm mã tra cứu."
                    : "Yêu cầu không hợp lệ.";
        };

        resp.setStatus(code);
        if (wantsJson(req, uri)) {
            resp.setContentType("application/json;charset=UTF-8");
            Json.mapper().writeValue(resp.getOutputStream(),
                    new ErrorResponse(code, "HTTP_" + code, message, Map.of(), requestId, Instant.now()));
        } else {
            req.setAttribute("errorStatus", code);
            req.setAttribute("errorMessage", message);
            req.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(req, resp);
        }
    }

    private static boolean wantsJson(HttpServletRequest req, String originalUri) {
        String path = originalUri == null ? "" : originalUri.substring(Math.min(req.getContextPath().length(), originalUri.length()));
        String accept = req.getHeader("Accept");
        return path.startsWith("/api/")
                || "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"));
    }
}
