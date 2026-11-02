package com.hrm.web.api;

import com.hrm.model.AttendanceFilter;
import com.hrm.model.AttendanceForm;
import com.hrm.model.AttendanceStatus;
import com.hrm.service.AttendanceService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * REST resource {@code /api/attendance}.
 * <pre>
 * GET  /api/attendance/today            my record today + server time
 * POST /api/attendance/check-in         POST /api/attendance/check-out
 * GET  /api/attendance/my?month=2026-10 my month: summary + records
 * GET  /api/attendance?from=&to=&departmentId=&employeeId=&status=&keyword=&page=&size=
 * POST /api/attendance                  create/correct one day (HR, time clock)
 * POST /api/attendance/bulk             up to 1000 punches, per-element result (time clock)
 * DELETE /api/attendance/{id}           (HR)
 * </pre>
 */
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
         .post("/bulk", ctx -> ctx.json(attendanceService.saveBatch(java.util.Arrays.asList(ctx.readJson(AttendanceForm[].class)))))
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
        boolean apiClient = com.hrm.security.AuthUser.API_CLIENT.equals(ctx.requireUser().getUsername());
        ctx.json(attendanceService.save(ctx.readJson(AttendanceForm.class), apiClient ? "API" : "MANUAL"));
    }

    private static YearMonth month(WebContext ctx) {
        String m = ctx.param("month");
        try {
            return m == null ? null : YearMonth.parse(m);
        } catch (DateTimeParseException e) {
            throw new com.hrm.exception.ValidationException("month", "Tháng không hợp lệ (yyyy-MM).");
        }
    }
}
