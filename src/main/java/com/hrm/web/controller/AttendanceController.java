package com.hrm.web.controller;

import com.hrm.service.DepartmentService;
import com.hrm.service.LeaveService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class AttendanceController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient DepartmentService departmentService;
    private final transient LeaveService leaveService;
    private final boolean leavePages;

    private AttendanceController(DepartmentService departmentService, LeaveService leaveService, boolean leavePages) {
        this.departmentService = departmentService;
        this.leaveService = leaveService;
        this.leavePages = leavePages;
    }

    public static AttendanceController attendance(DepartmentService departmentService) {
        return new AttendanceController(departmentService, null, false);
    }

    public static AttendanceController leave(LeaveService leaveService) {
        return new AttendanceController(null, leaveService, true);
    }

    @Override
    protected void routes(Router r) {
        if (leavePages) {
            r.get("/", ctx -> ctx.attr("leaveTypes", leaveService.leaveTypes()).render("leave/my"))
             .get("/approvals", ctx -> ctx.attr("leaveTypes", leaveService.leaveTypes()).render("leave/approvals"));
        } else {
            r.get("/", ctx -> ctx.render("attendance/my"))
             .get("/manage", ctx -> ctx.attr("departments", departmentService.list()).render("attendance/manage"));
        }
    }
}
