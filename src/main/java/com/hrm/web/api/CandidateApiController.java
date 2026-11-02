package com.hrm.web.api;

import com.hrm.model.CandidateForm;
import com.hrm.model.CandidateStage;
import com.hrm.model.HireRequest;
import com.hrm.security.AuthUser;
import com.hrm.exception.ForbiddenException;
import com.hrm.service.RecruitmentService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;

/**
 * {@code /api/candidates}: GET / · GET /{id} · POST / · PUT /{id} · DELETE /{id} ·
 * POST /{id}/stage {"stage":"INTERVIEW"} · POST /{id}/hire {HireRequest}.
 * Managers (in scope) may read and PUT rating/notes; every other write is HR only.
 */
public class CandidateApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient RecruitmentService service;

    public CandidateApiController(RecruitmentService service) {
        this.service = service;
    }

    /** Body of a stage change. */
    public record StageRequest(CandidateStage stage) {
    }

    @Override
    protected void routes(Router r) {
        r.get("/", this::search)
         .get("/{id}", ctx -> ctx.json(service.candidate(ctx.requireUser(), ctx.pathId("id"))))
         .post("/", ctx -> ctx.json(201, service.addCandidate(hrOnly(ctx).readJson(CandidateForm.class))))
         .put("/{id}", ctx -> ctx.json(service.updateCandidate(ctx.requireUser(), ctx.pathId("id"), ctx.readJson(CandidateForm.class))))
         .delete("/{id}", ctx -> {
             service.deleteCandidate(hrOnly(ctx).pathId("id"));
             ctx.noContent();
         })
         .post("/{id}/stage", ctx -> ctx.json(service.moveStage(hrOnly(ctx).requireUser(), ctx.pathId("id"),
                 ctx.readJson(StageRequest.class).stage())))
         .post("/{id}/hire", ctx -> ctx.json(201, service.hire(hrOnly(ctx).pathId("id"), ctx.readJson(HireRequest.class))));
    }

    private void search(WebContext ctx) throws IOException {
        String stage = ctx.param("stage");
        ctx.json(service.searchCandidates(ctx.requireUser(), ctx.longParam("jobId"),
                stage == null ? null : CandidateStage.valueOf(stage), ctx.param("keyword"), ctx.pageRequest(20)));
    }

    private static WebContext hrOnly(WebContext ctx) {
        AuthUser user = ctx.requireUser();
        if (!user.isHrOrAdmin()) {
            throw new ForbiddenException("Chỉ bộ phận nhân sự được thực hiện thao tác này.");
        }
        return ctx;
    }
}
