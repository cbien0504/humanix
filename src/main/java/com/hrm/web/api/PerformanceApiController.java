package com.hrm.web.api;

import com.hrm.model.ReviewCycleForm;
import com.hrm.model.ReviewStatus;
import com.hrm.model.ScoreSubmission;
import com.hrm.service.PerformanceService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class PerformanceApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient PerformanceService service;
    private final boolean cycles;

    private PerformanceApiController(PerformanceService service, boolean cycles) {
        this.service = service;
        this.cycles = cycles;
    }

    public static PerformanceApiController cycles(PerformanceService service) {
        return new PerformanceApiController(service, true);
    }

    public static PerformanceApiController reviews(PerformanceService service) {
        return new PerformanceApiController(service, false);
    }

    @Override
    protected void routes(Router r) {
        if (cycles) {
            r.get("/", ctx -> ctx.json(service.cycles()))
             .post("/", ctx -> ctx.json(201, service.createCycle(ctx.requireUser(), ctx.readJson(ReviewCycleForm.class))))
             .put("/{id}", ctx -> ctx.json(service.updateCycle(ctx.pathId("id"), ctx.readJson(ReviewCycleForm.class))))
             .delete("/{id}", ctx -> {
                 service.deleteCycle(ctx.pathId("id"));
                 ctx.noContent();
             })
             .post("/{id}/open", ctx -> ctx.json(service.openCycle(ctx.pathId("id"))))
             .post("/{id}/close", ctx -> ctx.json(service.closeCycle(ctx.pathId("id"))))
             .get("/{id}/stats", ctx -> ctx.json(service.cycleStats(ctx.pathId("id"))));
        } else {
            r.get("/my", ctx -> ctx.json(service.myReviews(ctx.requireUser(), ctx.pageRequest(20))))
             .get("/criteria", ctx -> ctx.json(service.criteria()))
             .get("/", ctx -> {
                 ctx.json(service.reviews(ctx.requireUser(), ctx.longParam("cycleId"),
                         ctx.enumParam("status", ReviewStatus.class), ctx.param("keyword"), ctx.pageRequest(20)));
             })
             .get("/{id}", ctx -> ctx.json(service.view(ctx.requireUser(), ctx.pathId("id"))))
             .post("/{id}/self", ctx -> ctx.json(service.submitSelf(ctx.requireUser(), ctx.pathId("id"),
                     ctx.readJson(ScoreSubmission.class))))
             .post("/{id}/manager", ctx -> ctx.json(service.submitManager(ctx.requireUser(), ctx.pathId("id"),
                     ctx.readJson(ScoreSubmission.class))));
        }
    }
}
