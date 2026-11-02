package com.hrm.web;

import java.time.Instant;
import java.util.Map;

/** JSON error body returned by every AJAX/API endpoint. */
public record ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors,
                            String requestId, Instant timestamp) {
}
