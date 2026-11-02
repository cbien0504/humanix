package com.hrm.web;

import com.hrm.exception.AppException;
import com.hrm.exception.DataIntegrityException;
import com.hrm.exception.ValidationException;
import com.hrm.web.filter.RequestContextFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Centralised exception handling for all controllers (NFR-10).
 * <ul>
 *   <li>{@link AppException} → its HTTP status and message (4xx: shown to the user).</li>
 *   <li>Unexpected exceptions / 5xx → logged with stack trace; the user only sees a generic
 *       message and the request id to quote to IT support.</li>
 *   <li>AJAX/API requests get an {@link ErrorResponse} JSON body; page requests get the
 *       error JSP.</li>
 * </ul>
 */
public final class ErrorHandler {

    public static final ErrorHandler INSTANCE = new ErrorHandler();
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);
    static final String GENERIC_MESSAGE = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại hoặc liên hệ bộ phận IT kèm mã tra cứu.";

    private ErrorHandler() {
    }

    public void handle(WebContext ctx, Throwable thrown) throws IOException {
        Throwable error = unwrap(thrown);
        HttpServletRequest req = ctx.request();
        HttpServletResponse resp = ctx.response();
        String requestId = RequestContextFilter.requestId(req);

        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = GENERIC_MESSAGE;
        Map<String, String> fieldErrors = Map.of();

        if (error instanceof AppException app) {
            status = app.getHttpStatus();
            code = app.getErrorCode();
            if (status < 500 || status == 503) {
                message = app.getMessage();
            }
            if (app instanceof ValidationException ve) {
                fieldErrors = ve.getFieldErrors();
            } else if (app instanceof DataIntegrityException di && di.getField() != null) {
                fieldErrors = Map.of(di.getField(), di.getMessage());
            }
        }

        String where = req.getMethod() + " " + req.getRequestURI();
        if (status >= 500) {
            if (status == 503) {
                log.error("{} -> {} {}: {}", where, status, code, rootMessage(error));
            } else {
                log.error("{} -> {} {}", where, status, code, error);
            }
        } else if (status == 401 || status == 403) {
            log.warn("{} -> {} {}: {}", where, status, code, error.getMessage());
        } else {
            log.info("{} -> {} {}: {}", where, status, code, error.getMessage());
        }

        if (resp.isCommitted()) {
            log.warn("Response already committed, cannot send error body for {}", where);
            return;
        }
        resp.resetBuffer();
        if (ctx.wantsJson()) {
            ctx.json(status, new ErrorResponse(status, code, message, fieldErrors, requestId, Instant.now()));
        } else {
            resp.setStatus(status);
            req.setAttribute("errorStatus", status);
            req.setAttribute("errorMessage", message);
            try {
                ctx.render("error/error");
            } catch (ServletException e) {
                log.error("Error page rendering failed", e);
                resp.sendError(status);
            }
        }
    }

    private static Throwable unwrap(Throwable t) {
        Throwable current = t;
        while (current instanceof ServletException se && se.getRootCause() != null) {
            current = se.getRootCause();
        }
        return current;
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
