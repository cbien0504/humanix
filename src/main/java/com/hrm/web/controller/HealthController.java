package com.hrm.web.controller;

import com.hrm.dao.jdbc.JdbcExecutor;
import com.hrm.exception.AppException;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HealthController extends BaseController {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final transient JdbcExecutor jdbc;
    private final transient Clock clock;
    private final String version;
    private final AtomicBoolean databaseUp = new AtomicBoolean(true);

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
        String cause = null;
        long start = System.nanoTime();
        try {
            jdbc.queryScalar("SELECT 1", Integer.class);
            up = true;
        } catch (AppException e) {
            up = false;
            db.put("error", e.getMessage());
            cause = rootMessage(e);
        }
        if (databaseUp.getAndSet(up) != up) {
            if (up) {
                log.info("Database reachable again");
            } else {
                log.error("Database unreachable: {}", cause);
            }
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

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
