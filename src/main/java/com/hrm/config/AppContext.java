package com.hrm.config;

import com.hrm.dao.AttendanceDao;
import com.hrm.dao.AuditDao;
import com.hrm.dao.DataQualityDao;
import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.HolidayDao;
import com.hrm.dao.LeaveDao;
import com.hrm.dao.PayrollDao;
import com.hrm.dao.PerformanceDao;
import com.hrm.dao.RecruitmentDao;
import com.hrm.dao.ReportDao;
import com.hrm.dao.PositionDao;
import com.hrm.dao.UserDao;
import com.hrm.dao.jdbc.JdbcAttendanceDao;
import com.hrm.dao.jdbc.JdbcAuditDao;
import com.hrm.dao.jdbc.JdbcDataQualityDao;
import com.hrm.dao.jdbc.JdbcDepartmentDao;
import com.hrm.dao.jdbc.JdbcEmployeeDao;
import com.hrm.dao.jdbc.JdbcExecutor;
import com.hrm.dao.jdbc.JdbcHolidayDao;
import com.hrm.dao.jdbc.JdbcLeaveDao;
import com.hrm.dao.jdbc.JdbcPayrollDao;
import com.hrm.dao.jdbc.JdbcPerformanceDao;
import com.hrm.dao.jdbc.JdbcRecruitmentDao;
import com.hrm.dao.jdbc.JdbcReportDao;
import com.hrm.dao.jdbc.JdbcPositionDao;
import com.hrm.dao.jdbc.JdbcUserDao;
import com.hrm.security.AccessPolicy;
import com.hrm.security.BCryptPasswordHasher;
import com.hrm.security.PasswordHasher;
import com.hrm.security.PasswordPolicy;
import com.hrm.security.UserContext;
import com.hrm.service.AttendancePolicy;
import com.hrm.service.AttendanceService;
import com.hrm.service.AuditService;
import com.hrm.service.AuthService;
import com.hrm.service.DashboardService;
import com.hrm.service.DataQualityService;
import com.hrm.service.DepartmentService;
import com.hrm.service.EmployeeImportService;
import com.hrm.service.EmployeeService;
import com.hrm.service.LeaveService;
import com.hrm.service.PayrollCalculator;
import com.hrm.service.PayrollService;
import com.hrm.service.PerformanceService;
import com.hrm.service.RecruitmentService;
import com.hrm.service.ReportService;
import com.hrm.service.PositionService;
import com.hrm.service.UserService;
import com.zaxxer.hikari.HikariDataSource;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

public final class AppContext implements AutoCloseable {

    public static final String ATTRIBUTE = AppContext.class.getName();

    private final AppConfig config;
    private final Clock clock;
    private final HikariDataSource dataSource;
    private final JdbcExecutor jdbc;

    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final AccessPolicy accessPolicy = new AccessPolicy();

    private final AuditService auditService;
    private final AuthService authService;
    private final UserService userService;
    private final DataQualityService dataQualityService;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final RecruitmentService recruitmentService;
    private final PerformanceService performanceService;
    private final ReportService reportService;
    private final DashboardService dashboardService;
    private final EmployeeImportService importService;

    private AppContext(AppConfig config) {
        this.config = config;
        ZoneId zone = config.getZone("app.timezone", ZoneId.of("Asia/Ho_Chi_Minh"));
        this.clock = Clock.system(zone);
        this.dataSource = DataSourceFactory.create(config);
        this.jdbc = new JdbcExecutor(dataSource, UserContext::currentUsername, config.getLong("db.slowQueryMs", 500));

        UserDao userDao = new JdbcUserDao(jdbc);
        AuditDao auditDao = new JdbcAuditDao(jdbc, zone);
        DataQualityDao dataQualityDao = new JdbcDataQualityDao(jdbc);
        DepartmentDao departmentDao = new JdbcDepartmentDao(jdbc);
        PositionDao positionDao = new JdbcPositionDao(jdbc);
        EmployeeDao employeeDao = new JdbcEmployeeDao(jdbc);
        HolidayDao holidayDao = new JdbcHolidayDao(jdbc);
        AttendanceDao attendanceDao = new JdbcAttendanceDao(jdbc);
        LeaveDao leaveDao = new JdbcLeaveDao(jdbc);
        PayrollDao payrollDao = new JdbcPayrollDao(jdbc);
        RecruitmentDao recruitmentDao = new JdbcRecruitmentDao(jdbc);
        PerformanceDao performanceDao = new JdbcPerformanceDao(jdbc);
        ReportDao reportDao = new JdbcReportDao(jdbc);

        this.passwordHasher = new BCryptPasswordHasher(config.getInt("security.bcryptCost", 10));
        this.passwordPolicy = new PasswordPolicy(config.getInt("security.passwordMinLength", 8));

        this.auditService = new AuditService(auditDao);
        this.authService = new AuthService(userDao, passwordHasher, passwordPolicy, auditService, clock,
                config.getInt("security.maxFailedLogins", 5), config.getInt("security.lockMinutes", 15));
        this.userService = new UserService(userDao, passwordHasher, passwordPolicy, auditService, jdbc);
        this.dataQualityService = new DataQualityService(dataQualityDao, auditService);
        this.departmentService = new DepartmentService(departmentDao, employeeDao, auditService);
        this.positionService = new PositionService(positionDao, auditService);
        this.employeeService = new EmployeeService(employeeDao, departmentDao, positionDao, auditService, jdbc, clock);
        AttendancePolicy attendancePolicy = new AttendancePolicy(
                config.getTime("attendance.workStart", LocalTime.of(8, 30)),
                config.getInt("attendance.lateGraceMinutes", 5),
                config.getInt("attendance.halfDayMaxHours", 4));
        this.attendanceService = new AttendanceService(attendanceDao, employeeDao, leaveDao, attendancePolicy, auditService, clock);
        this.leaveService = new LeaveService(leaveDao, employeeDao, holidayDao, auditService, clock,
                config.getInt("leave.maxPastDays", 30));
        this.payrollService = new PayrollService(payrollDao, new PayrollCalculator(), auditService, clock);
        this.recruitmentService = new RecruitmentService(recruitmentDao, departmentDao, positionDao, employeeService,
                auditService, jdbc, clock);
        this.performanceService = new PerformanceService(performanceDao, auditService, jdbc);
        this.reportService = new ReportService(reportDao, departmentDao, employeeDao, auditService, clock);
        this.dashboardService = new DashboardService(reportDao, attendanceDao, leaveDao, payrollDao, performanceDao,
                recruitmentDao, clock);
        this.importService = new EmployeeImportService(employeeService, employeeDao, departmentDao, positionDao, jdbc, auditService);
    }

    public static AppContext create(AppConfig config) {
        return new AppContext(config);
    }

    public AppConfig config() {
        return config;
    }

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

    public AttendanceService attendanceService() {
        return attendanceService;
    }

    public LeaveService leaveService() {
        return leaveService;
    }

    public PayrollService payrollService() {
        return payrollService;
    }

    public RecruitmentService recruitmentService() {
        return recruitmentService;
    }

    public PerformanceService performanceService() {
        return performanceService;
    }

    public ReportService reportService() {
        return reportService;
    }

    public DashboardService dashboardService() {
        return dashboardService;
    }

    public EmployeeImportService importService() {
        return importService;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
