package com.hrm.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static HikariDataSource create(AppConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("hrm-pool");
        hc.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        hc.setJdbcUrl(config.require("db.url"));
        hc.setUsername(config.require("db.username"));
        hc.setPassword(config.get("db.password", ""));
        hc.setMaximumPoolSize(config.getInt("db.pool.maximumPoolSize", 10));
        hc.setMinimumIdle(config.getInt("db.pool.minimumIdle", 2));
        hc.setConnectionTimeout(config.getLong("db.pool.connectionTimeoutMs", 5_000));
        hc.setIdleTimeout(config.getLong("db.pool.idleTimeoutMs", 300_000));
        hc.setMaxLifetime(config.getLong("db.pool.maxLifetimeMs", 1_800_000));
        hc.setLeakDetectionThreshold(config.getLong("db.pool.leakDetectionThresholdMs", 0));
        hc.setInitializationFailTimeout(-1);
        hc.setAutoCommit(true);
        return new HikariDataSource(hc);
    }
}
