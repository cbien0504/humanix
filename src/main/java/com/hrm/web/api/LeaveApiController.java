package com.hrm.web.api;

import com.hrm.model.LeaveFilter;
import com.hrm.model.LeaveRequestForm;
import com.hrm.model.LeaveStatus;
import com.hrm.service.LeaveService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

public class LeaveApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient LeaveService leaveService;
    private final transient Clock clock;

    public LeaveApiController(LeaveService leaveService, Clock clock) {
        this.leaveService = leaveService;
        this.clock = clock;
    }

    public record Decision(String comment) {
    }

    @Override
    protected void routes(Router r) {
        r.get("/types", ctx -> ctx.json(leaveService.leaveTypes()))
         .get("/balances", ctx -> ctx.json(leaveService.balances(ctx.requireUser(), year(ctx))))
         .get("/working-days", ctx -> ctx.json(Map.of("days", leaveService.workingDays(ctx.dateParam("from"), ctx.dateParam("to")))))
         .get("/my", ctx -> ctx.json(leaveService.myRequests(ctx.requireUser(), filter(ctx), ctx.pageRequest(10))))
         .post("/", ctx -> ctx.json(201, leaveService.submit(ctx.requireUser(), ctx.readJson(LeaveRequestForm.class))))
         .post("/{id}/cancel", ctx -> ctx.json(leaveService.cancel(ctx.requireUser(), ctx.pathId("id"))))
         .get("/approvals", ctx -> ctx.json(leaveService.approvalQueue(ctx.requireUser(), filter(ctx), ctx.pageRequest(10))))
         .post("/{id}/approve", ctx -> ctx.json(leaveService.approve(ctx.requireUser(), ctx.pathId("id"),
                 ctx.readJson(Decision.class).comment())))
         .post("/{id}/reject", ctx -> ctx.json(leaveService.reject(ctx.requireUser(), ctx.pathId("id"),
                 ctx.readJson(Decision.class).comment())))
         .post("/balances/init", ctx -> ctx.json(Map.of("rows", leaveService.initBalances(year(ctx)))));
    }

    private LeaveFilter filter(WebContext ctx) {
        return new LeaveFilter(ctx.param("keyword"), ctx.enumParam("status", LeaveStatus.class),
                ctx.longParam("leaveTypeId"), ctx.intParam("year"), null, null);
    }

    private int year(WebContext ctx) throws IOException {
        Integer y = ctx.intParam("year");
        return y == null ? LocalDate.now(clock).getYear() : y;
    }
}
