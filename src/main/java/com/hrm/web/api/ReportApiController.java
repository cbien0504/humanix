package com.hrm.web.api;

import com.hrm.service.DashboardService;
import com.hrm.service.ReportService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class ReportApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient ReportService reportService;
    private final transient DashboardService dashboardService;

    private ReportApiController(ReportService reportService, DashboardService dashboardService) {
        this.reportService = reportService;
        this.dashboardService = dashboardService;
    }

    public static ReportApiController reports(ReportService service) {
        return new ReportApiController(service, null);
    }

    public static ReportApiController dashboard(DashboardService service) {
        return new ReportApiController(null, service);
    }

    @Override
    protected void routes(Router r) {
        if (dashboardService != null) {
            r.get("/", ctx -> ctx.json(dashboardService.dashboard(ctx.requireUser())));
            return;
        }
        r.get("/departments", ctx -> ctx.json(reportService.departmentStatistics(ctx.requireUser(), ctx.dateParam("asOf"))))
         .get("/attendance", ctx -> ctx.json(reportService.attendanceReport(ctx.requireUser(), ctx.dateParam("from"),
                 ctx.dateParam("to"), ctx.longParam("departmentId"))));
    }
}
