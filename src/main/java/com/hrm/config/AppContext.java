package com.hrm.config;

import com.hrm.dao.jdbc.JdbcExecutor;
import com.hrm.security.UserContext;
import com.zaxxer.hikari.HikariDataSource;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Composition root: creates and wires every long-lived object of the application with plain
 * constructor injection (no DI framework). Controllers and filters receive their dependencies
 * from here in {@link AppBootstrap}; tests build the same classes with mocks instead.
 */
public final class AppContext implements AutoCloseable {

    public static final String ATTRIBUTE = AppContext.class.getName();

    private final AppConfig config;
    private final Clock clock;
    private final HikariDataSource dataSource;
    private final JdbcExecutor jdbc;

    private AppContext(AppConfig config) {
        this.config = config;
        ZoneId zone = config.getZone("app.timezone", ZoneId.of("Asia/Ho_Chi_Minh"));
        this.clock = Clock.system(zone);
        this.dataSource = DataSourceFactory.create(config);
        this.jdbc = new JdbcExecutor(dataSource, UserContext::currentUsername, config.getLong("db.slowQueryMs", 500));
    }

    public static AppContext create(AppConfig config) {
        return new AppContext(config);
    }

    public AppConfig config() {
        return config;
    }

    /** Company clock (Asia/Ho_Chi_Minh) – inject it instead of calling LocalDate.now(). */
    public Clock clock() {
        return clock;
    }

    public JdbcExecutor jdbc() {
        return jdbc;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
