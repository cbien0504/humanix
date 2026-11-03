package com.hrm.web.api;

import com.hrm.exception.ValidationException;
import com.hrm.model.AttendanceFilter;
import com.hrm.model.AttendanceForm;
import com.hrm.model.AttendanceStatus;
import com.hrm.security.AuthUser;
import com.hrm.service.AttendanceService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

public class AttendanceApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient AttendanceService attendanceService;

    public AttendanceApiController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/today", ctx -> ctx.json(attendanceService.today(ctx.requireUser())))
         .post("/check-in", ctx -> ctx.json(attendanceService.checkIn(ctx.requireUser())))
         .post("/check-out", ctx -> ctx.json(attendanceService.checkOut(ctx.requireUser())))
         .get("/my", ctx -> ctx.json(attendanceService.myMonth(ctx.requireUser(), month(ctx))))
         .get("/", this::search)
         .post("/", this::save)
         .post("/bulk", ctx -> ctx.json(attendanceService.saveBatch(Arrays.asList(ctx.readJson(AttendanceForm[].class)))))
         .delete("/{id}", ctx -> {
             attendanceService.delete(ctx.pathId("id"));
             ctx.noContent();
         });
    }

    private void search(WebContext ctx) throws IOException {
        AttendanceFilter filter = new AttendanceFilter(ctx.param("keyword"), ctx.longParam("departmentId"),
                ctx.longParam("employeeId"), ctx.dateParam("from"), ctx.dateParam("to"),
                ctx.enumParam("status", AttendanceStatus.class), null);
        ctx.json(attendanceService.search(ctx.requireUser(), filter, ctx.pageRequest(20)));
    }

    private void save(WebContext ctx) throws IOException {
        boolean apiClient = AuthUser.API_CLIENT.equals(ctx.requireUser().getUsername());
        ctx.json(attendanceService.save(ctx.readJson(AttendanceForm.class), apiClient ? "API" : "MANUAL"));
    }

    private static YearMonth month(WebContext ctx) {
        String m = ctx.param("month");
        try {
            return m == null ? null : YearMonth.parse(m);
        } catch (DateTimeParseException e) {
            throw new ValidationException("month", "Tháng không hợp lệ (yyyy-MM).");
        }
    }
}
