package com.hrm.web.controller;

import com.hrm.service.DepartmentService;
import com.hrm.service.PositionService;
import com.hrm.service.RecruitmentService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class RecruitmentController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient RecruitmentService recruitmentService;
    private final transient DepartmentService departmentService;
    private final transient PositionService positionService;

    public RecruitmentController(RecruitmentService recruitmentService, DepartmentService departmentService,
                                 PositionService positionService) {
        this.recruitmentService = recruitmentService;
        this.departmentService = departmentService;
        this.positionService = positionService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", ctx -> ctx.attr("departments", departmentService.list()).attr("positions", positionService.list())
                 .render("recruitment/jobs"))
         .get("/jobs/{id}", ctx -> ctx.attr("job", recruitmentService.job(ctx.requireUser(), ctx.pathId("id")))
                 .render("recruitment/pipeline"));
    }
}
