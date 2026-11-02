package com.hrm.web.controller;

import com.hrm.model.Employee;
import com.hrm.model.EmployeeStatus;
import com.hrm.security.AuthUser;
import com.hrm.service.DepartmentService;
import com.hrm.service.EmployeeService;
import com.hrm.service.PositionService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;
import jakarta.servlet.ServletException;

import java.io.IOException;

/** {@code /employees}: employee list page and server-rendered profile page. */
public class EmployeeController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient EmployeeService employeeService;
    private final transient DepartmentService departmentService;
    private final transient PositionService positionService;

    public EmployeeController(EmployeeService employeeService, DepartmentService departmentService,
                              PositionService positionService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.positionService = positionService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", this::list).get("/{id}", this::detail);
    }

    private void list(WebContext ctx) throws ServletException, IOException {
        ctx.attr("departments", departmentService.list())
           .attr("positions", positionService.list())
           .attr("statuses", EmployeeStatus.values())
           .render("employees/list");
    }

    private void detail(WebContext ctx) throws ServletException, IOException {
        AuthUser user = ctx.requireUser();
        Employee employee = employeeService.get(user, ctx.pathId("id"));
        showProfile(ctx, user, employee, employeeService);
    }

    /** Shared by "/employees/{id}" and "/profile". */
    static void showProfile(WebContext ctx, AuthUser user, Employee employee, EmployeeService service)
            throws ServletException, IOException {
        boolean self = user.getEmployeeId() != null && user.getEmployeeId() == employee.getEmployeeId();
        ctx.attr("employee", employee).attr("self", self);
        if (user.isHrOrAdmin() || self) {
            ctx.attr("salaryHistory", service.salaryHistory(user, employee.getEmployeeId()));
        }
        ctx.render("employees/detail");
    }
}
