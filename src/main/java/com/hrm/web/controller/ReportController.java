package com.hrm.web.controller;

import com.hrm.model.EmployeeFilter;
import com.hrm.model.EmployeeStatus;
import com.hrm.service.DepartmentService;
import com.hrm.service.ReportService;
import com.hrm.web.BaseController;
import com.hrm.web.Downloads;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;
import java.time.LocalDate;

/**
 * {@code /reports}: report page and Excel/CSV downloads
 * ({@code /reports/departments/export}, {@code /reports/attendance/export}, {@code /reports/employees/export}).
 */
public class ReportController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient ReportService reportService;
    private final transient DepartmentService departmentService;

    public ReportController(ReportService reportService, DepartmentService departmentService) {
        this.reportService = reportService;
        this.departmentService = departmentService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", ctx -> ctx.attr("departments", departmentService.list()).render("reports/index"))
         .get("/departments/export", ctx -> Downloads.report(ctx,
                 reportService.departmentReport(ctx.requireUser(), ctx.dateParam("asOf")), "thong-ke-phong-ban-" + LocalDate.now()))
         .get("/attendance/export", ctx -> Downloads.report(ctx, reportService.attendanceReportTable(ctx.requireUser(),
                 ctx.dateParam("from"), ctx.dateParam("to"), ctx.longParam("departmentId")), "bao-cao-cham-cong-" + LocalDate.now()))
         .get("/employees/export", this::employees);
    }

    private void employees(WebContext ctx) throws IOException {
        EmployeeFilter filter = new EmployeeFilter(ctx.param("keyword"), ctx.longParam("departmentId"), ctx.longParam("positionId"),
                ctx.enumParam("status", EmployeeStatus.class), null);
        Downloads.report(ctx, reportService.employeeList(filter), "danh-sach-nhan-vien-" + LocalDate.now());
    }
}
