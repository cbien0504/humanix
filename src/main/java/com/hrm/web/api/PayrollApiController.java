package com.hrm.web.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hrm.exception.ValidationException;
import com.hrm.service.PayrollService;
import com.hrm.util.Json;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * REST resource {@code /api/payroll}.
 * <pre>
 * GET  /periods?year=              POST /periods/calculate {"year":2026,"month":10}
 * POST /periods/{id}/approve       POST /periods/{id}/pay
 * GET  /periods/{id}/payslips?keyword=&departmentId=&page=   GET /periods/{id}/departments
 * GET  /my                         GET  /payslips/{id}
 * POST /simulate {"baseSalary":..,"allowance":..,"dependents":..}
 * GET  /settings                   PUT  /settings {"PERSONAL_DEDUCTION": 15500000, ...}   (ADMIN)
 * </pre>
 */
public class PayrollApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient PayrollService payrollService;

    public PayrollApiController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    /** Body of the calculate call. */
    public record CalculateRequest(int year, int month) {
    }

    @Override
    protected void routes(Router r) {
        r.get("/periods", ctx -> ctx.json(payrollService.periods(ctx.intParam("year"))))
         .post("/periods/calculate", ctx -> {
             CalculateRequest req = ctx.readJson(CalculateRequest.class);
             ctx.json(payrollService.calculate(ctx.requireUser(), req.year(), req.month()));
         })
         .post("/periods/{id}/approve", ctx -> ctx.json(payrollService.approve(ctx.requireUser(), ctx.pathId("id"))))
         .post("/periods/{id}/pay", ctx -> ctx.json(payrollService.markPaid(ctx.requireUser(), ctx.pathId("id"))))
         .get("/periods/{id}/payslips", ctx -> ctx.json(payrollService.payslips(ctx.pathId("id"), ctx.param("keyword"),
                 ctx.longParam("departmentId"), ctx.pageRequest(20))))
         .get("/periods/{id}/departments", ctx -> ctx.json(payrollService.departmentTotals(ctx.pathId("id"))))
         .get("/my", ctx -> ctx.json(payrollService.myPayslips(ctx.requireUser())))
         .get("/payslips/{id}", ctx -> ctx.json(payrollService.payslip(ctx.requireUser(), ctx.pathId("id"))))
         .post("/simulate", ctx -> ctx.json(payrollService.simulate(ctx.readJson(PayrollService.SimulationRequest.class))))
         .get("/settings", ctx -> ctx.json(Map.of("settings", payrollService.settingDefinitions(),
                 "taxBrackets", payrollService.settings().taxBrackets())))
         .put("/settings", this::updateSettings);
    }

    private void updateSettings(WebContext ctx) throws IOException {
        Map<String, BigDecimal> changes;
        try {
            changes = Json.mapper().readValue(ctx.request().getInputStream(), new TypeReference<Map<String, BigDecimal>>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ValidationException("body", "Dữ liệu tham số không hợp lệ.");
        }
        ctx.json(payrollService.updateSettings(changes));
    }
}
