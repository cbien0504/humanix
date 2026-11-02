package com.hrm.web.controller;

import com.hrm.dao.jdbc.JdbcExecutor;
import com.hrm.exception.AppException;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code GET /health} – liveness/readiness probe for Docker, load balancers and monitoring.
 * Returns 200 when the database answers, 503 otherwise (never throws).
 */
public class HealthController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient JdbcExecutor jdbc;
    private final transient Clock clock;
    private final String version;

    public HealthController(JdbcExecutor jdbc, Clock clock, String version) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.version = version;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", this::health);
    }

    private void health(WebContext ctx) throws IOException {
        Map<String, Object> db = new LinkedHashMap<>();
        boolean up;
        long start = System.nanoTime();
        try {
            jdbc.queryScalar("SELECT 1", Integer.class);
            up = true;
        } catch (AppException e) {
            up = false;
            db.put("error", e.getMessage());
        }
        db.put("status", up ? "UP" : "DOWN");
        db.put("latencyMs", (System.nanoTime() - start) / 1_000_000);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", up ? "UP" : "DOWN");
        body.put("version", version);
        body.put("time", Instant.now(clock));
        body.put("database", db);
        ctx.json(up ? 200 : 503, body);
    }
}
