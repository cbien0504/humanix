package com.hrm.web.controller;

import com.hrm.model.PayrollPeriod;
import com.hrm.service.DepartmentService;
import com.hrm.service.PayrollService;
import com.hrm.web.BaseController;
import com.hrm.web.Downloads;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;

public class PayrollController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient PayrollService payrollService;
    private final transient DepartmentService departmentService;

    public PayrollController(PayrollService payrollService, DepartmentService departmentService) {
        this.payrollService = payrollService;
        this.departmentService = departmentService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", ctx -> ctx.render("payroll/periods"))
         .get("/my", ctx -> ctx.render("payroll/my"))
         .get("/payslip/{id}", ctx -> ctx.attr("p", payrollService.payslip(ctx.requireUser(), ctx.pathId("id")))
                 .render("payroll/payslip"))
         .get("/{id}", ctx -> ctx.attr("period", payrollService.period(ctx.pathId("id")))
                 .attr("departments", departmentService.list()).render("payroll/period"))
         .get("/{id}/export", this::export);
    }

    private void export(WebContext ctx) throws IOException {
        long id = ctx.pathId("id");
        PayrollPeriod p = payrollService.period(id);
        Downloads.report(ctx, payrollService.payrollSheet(id),
                String.format("bang-luong-%d-%02d", p.getPeriodYear(), p.getPeriodMonth()));
    }
}
