package com.hrm.it;

import com.hrm.config.AppConfig;
import com.hrm.config.DataSourceFactory;
import com.hrm.dao.jdbc.JdbcExecutor;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Assumptions;

import java.util.Map;

final class ItDatabase {

    static final String AUDIT_USER = "it-test";

    private ItDatabase() {
    }

    static HikariDataSource open() {
        String url = System.getenv("HRM_IT_DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "HRM_IT_DB_URL is not set – integration test skipped");
        return DataSourceFactory.create(AppConfig.of(Map.of(
                "db.url", url,
                "db.username", env("HRM_IT_DB_USER", "hrm_app"),
                "db.password", env("HRM_IT_DB_PASSWORD", ""),
                "db.pool.maximumPoolSize", "2",
                "db.pool.minimumIdle", "0")));
    }

    static JdbcExecutor executor(HikariDataSource dataSource) {
        return new JdbcExecutor(dataSource, () -> AUDIT_USER, 10_000);
    }

    static void rollbackAfter(JdbcExecutor jdbc, Runnable work) {
        try {
            jdbc.inTransaction(() -> {
                work.run();
                throw new Rollback();
            });
        } catch (Rollback expected) {
        }
    }

    private static String env(String name, String defaultValue) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? defaultValue : v;
    }

    private static final class Rollback extends RuntimeException {
        Rollback() {
            super(null, null, false, false);
        }
    }
}
