package com.hrm.config;

import com.hrm.dao.AuditDao;
import com.hrm.dao.DataQualityDao;
import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.PositionDao;
import com.hrm.dao.UserDao;
import com.hrm.dao.jdbc.JdbcAuditDao;
import com.hrm.dao.jdbc.JdbcDataQualityDao;
import com.hrm.dao.jdbc.JdbcDepartmentDao;
import com.hrm.dao.jdbc.JdbcEmployeeDao;
import com.hrm.dao.jdbc.JdbcExecutor;
import com.hrm.dao.jdbc.JdbcPositionDao;
import com.hrm.dao.jdbc.JdbcUserDao;
import com.hrm.security.AccessPolicy;
import com.hrm.security.BCryptPasswordHasher;
import com.hrm.security.PasswordHasher;
import com.hrm.security.PasswordPolicy;
import com.hrm.security.UserContext;
import com.hrm.service.AuditService;
import com.hrm.service.AuthService;
import com.hrm.service.DataQualityService;
import com.hrm.service.DepartmentService;
import com.hrm.service.EmployeeService;
import com.hrm.service.PositionService;
import com.hrm.service.UserService;
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

    // security
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final AccessPolicy accessPolicy = new AccessPolicy();

    // services
    private final AuditService auditService;
    private final AuthService authService;
    private final UserService userService;
    private final DataQualityService dataQualityService;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final EmployeeService employeeService;

    private AppContext(AppConfig config) {
        this.config = config;
        ZoneId zone = config.getZone("app.timezone", ZoneId.of("Asia/Ho_Chi_Minh"));
        this.clock = Clock.system(zone);
        this.dataSource = DataSourceFactory.create(config);
        this.jdbc = new JdbcExecutor(dataSource, UserContext::currentUsername, config.getLong("db.slowQueryMs", 500));

        // DAOs
        UserDao userDao = new JdbcUserDao(jdbc);
        AuditDao auditDao = new JdbcAuditDao(jdbc, zone);
        DataQualityDao dataQualityDao = new JdbcDataQualityDao(jdbc);
        DepartmentDao departmentDao = new JdbcDepartmentDao(jdbc);
        PositionDao positionDao = new JdbcPositionDao(jdbc);
        EmployeeDao employeeDao = new JdbcEmployeeDao(jdbc);

        // security
        this.passwordHasher = new BCryptPasswordHasher(config.getInt("security.bcryptCost", 10));
        this.passwordPolicy = new PasswordPolicy(config.getInt("security.passwordMinLength", 8));

        // services
        this.auditService = new AuditService(auditDao);
        this.authService = new AuthService(userDao, passwordHasher, passwordPolicy, auditService, clock,
                config.getInt("security.maxFailedLogins", 5), config.getInt("security.lockMinutes", 15));
        this.userService = new UserService(userDao, passwordHasher, passwordPolicy, auditService, jdbc);
        this.dataQualityService = new DataQualityService(dataQualityDao, auditService);
        this.departmentService = new DepartmentService(departmentDao, employeeDao, auditService);
        this.positionService = new PositionService(positionDao, auditService);
        this.employeeService = new EmployeeService(employeeDao, departmentDao, positionDao, auditService, jdbc, clock);
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

    public AccessPolicy accessPolicy() {
        return accessPolicy;
    }

    public AuditService auditService() {
        return auditService;
    }

    public AuthService authService() {
        return authService;
    }

    public UserService userService() {
        return userService;
    }

    public DataQualityService dataQualityService() {
        return dataQualityService;
    }

    public DepartmentService departmentService() {
        return departmentService;
    }

    public PositionService positionService() {
        return positionService;
    }

    public EmployeeService employeeService() {
        return employeeService;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
