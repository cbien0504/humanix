package com.hrm.dao.jdbc;

import com.hrm.config.AppConfig;
import com.hrm.config.DataSourceFactory;
import com.hrm.exception.DatabaseUnavailableException;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseOutageTest {

    @Test
    void anUnreachableDatabaseIsReportedAsUnavailableWithinTheConnectionTimeout() {
        AppConfig config = AppConfig.of(Map.of(
                "db.url", "jdbc:sqlserver://127.0.0.1:1;databaseName=HRM;encrypt=false;loginTimeout=1",
                "db.username", "hrm_app",
                "db.pool.minimumIdle", "0",
                "db.pool.connectionTimeoutMs", "500"));

        try (HikariDataSource pool = DataSourceFactory.create(config)) {
            JdbcExecutor jdbc = new JdbcExecutor(pool, () -> "test", 1_000);
            long start = System.nanoTime();

            assertThatThrownBy(() -> jdbc.count("SELECT COUNT(*) FROM dbo.Employees"))
                    .isInstanceOfSatisfying(DatabaseUnavailableException.class, e -> {
                        assertThat(e.getHttpStatus()).isEqualTo(503);
                        assertThat(e.getMessage()).contains("không kết nối được cơ sở dữ liệu");
                    });
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        }
    }
}
