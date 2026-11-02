package com.hrm.web.api;

import com.hrm.model.JobPostingForm;
import com.hrm.model.JobStatus;
import com.hrm.service.RecruitmentService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;

/**
 * {@code /api/jobs}: GET / · GET /{id} · GET /{id}/pipeline · POST / · PUT /{id} ·
 * POST /{id}/status {"status":"OPEN"} · DELETE /{id}. Writes require HR (AccessPolicy).
 */
public class JobApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient RecruitmentService service;

    public JobApiController(RecruitmentService service) {
        this.service = service;
    }

    /** Body of the status change. */
    public record StatusChange(JobStatus status) {
    }

    @Override
    protected void routes(Router r) {
        r.get("/", this::search)
         .get("/stats", ctx -> ctx.json(service.stats()))
         .get("/{id}", ctx -> ctx.json(service.job(ctx.requireUser(), ctx.pathId("id"))))
         .get("/{id}/pipeline", ctx -> ctx.json(service.pipeline(ctx.requireUser(), ctx.pathId("id"))))
         .post("/", ctx -> ctx.json(201, service.createJob(ctx.requireUser(), ctx.readJson(JobPostingForm.class))))
         .put("/{id}", ctx -> ctx.json(service.updateJob(ctx.pathId("id"), ctx.readJson(JobPostingForm.class))))
         .post("/{id}/status", ctx -> ctx.json(service.changeJobStatus(ctx.pathId("id"), ctx.readJson(StatusChange.class).status())))
         .delete("/{id}", ctx -> {
             service.deleteJob(ctx.pathId("id"));
             ctx.noContent();
         });
    }

    private void search(WebContext ctx) throws IOException {
        ctx.json(service.searchJobs(ctx.requireUser(), ctx.param("keyword"), ctx.enumParam("status", JobStatus.class),
                ctx.longParam("departmentId"), ctx.pageRequest(10)));
    }
}
