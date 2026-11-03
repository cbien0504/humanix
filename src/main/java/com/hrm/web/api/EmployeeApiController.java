package com.hrm.web.api;

import com.hrm.model.EmployeeFilter;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.service.EmployeeService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;
import java.util.Map;

public class EmployeeApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient EmployeeService employeeService;

    public EmployeeApiController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", this::search)
         .get("/options", ctx -> ctx.json(employeeService.options()))
         .get("/next-code", ctx -> ctx.json(Map.of("employeeCode", employeeService.nextCode())))
         .get("/by-code/{code}", ctx -> ctx.json(employeeService.getByCode(ctx.requireUser(), ctx.pathVar("code"))))
         .get("/{id}", ctx -> ctx.json(employeeService.get(ctx.requireUser(), ctx.pathId("id"))))
         .get("/{id}/salary-history", ctx -> ctx.json(employeeService.salaryHistory(ctx.requireUser(), ctx.pathId("id"))))
         .post("/", this::create)
         .put("/{id}", ctx -> ctx.json(employeeService.update(ctx.pathId("id"), ctx.readJson(EmployeeForm.class))))
         .delete("/{id}", ctx -> {
             employeeService.delete(ctx.pathId("id"));
             ctx.noContent();
         });
    }

    private void search(WebContext ctx) throws IOException {
        EmployeeFilter filter = new EmployeeFilter(ctx.param("keyword"), ctx.longParam("departmentId"),
                ctx.longParam("positionId"), ctx.enumParam("status", EmployeeStatus.class), null);
        ctx.json(employeeService.search(ctx.requireUser(), filter, ctx.pageRequest(10)));
    }

    private void create(WebContext ctx) throws IOException {
        var created = employeeService.create(ctx.readJson(EmployeeForm.class));
        ctx.response().setHeader("Location", ctx.request().getContextPath() + "/api/employees/" + created.getEmployeeId());
        ctx.json(201, created);
    }
}
