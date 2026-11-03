package com.hrm.web.controller;

import com.hrm.service.PerformanceService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class PerformanceController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", ctx -> ctx.render("performance/my"))
         .get("/manage", ctx -> ctx.attr("cycles", performanceService.cycles()).render("performance/manage"))
         .get("/reviews/{id}", ctx -> ctx.attr("view", performanceService.view(ctx.requireUser(), ctx.pathId("id")))
                 .render("performance/review"));
    }
}
