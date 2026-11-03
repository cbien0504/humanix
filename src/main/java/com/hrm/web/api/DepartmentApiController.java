package com.hrm.web.api;

import com.hrm.model.DepartmentForm;
import com.hrm.model.PositionForm;
import com.hrm.service.DepartmentService;
import com.hrm.service.PositionService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class DepartmentApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient DepartmentService departmentService;
    private final transient PositionService positionService;
    private final boolean positions;

    private DepartmentApiController(DepartmentService departmentService, PositionService positionService, boolean positions) {
        this.departmentService = departmentService;
        this.positionService = positionService;
        this.positions = positions;
    }

    public static DepartmentApiController departments(DepartmentService service) {
        return new DepartmentApiController(service, null, false);
    }

    public static DepartmentApiController positions(PositionService service) {
        return new DepartmentApiController(null, service, true);
    }

    @Override
    protected void routes(Router r) {
        if (positions) {
            r.get("/", ctx -> ctx.json(positionService.list()))
             .get("/{id}", ctx -> ctx.json(positionService.get(ctx.pathId("id"))))
             .post("/", ctx -> ctx.json(201, positionService.create(ctx.readJson(PositionForm.class))))
             .put("/{id}", ctx -> ctx.json(positionService.update(ctx.pathId("id"), ctx.readJson(PositionForm.class))))
             .delete("/{id}", ctx -> {
                 positionService.delete(ctx.pathId("id"));
                 ctx.noContent();
             });
        } else {
            r.get("/", ctx -> ctx.json(departmentService.list()))
             .get("/{id}", ctx -> ctx.json(departmentService.get(ctx.pathId("id"))))
             .post("/", ctx -> ctx.json(201, departmentService.create(ctx.readJson(DepartmentForm.class))))
             .put("/{id}", ctx -> ctx.json(departmentService.update(ctx.pathId("id"), ctx.readJson(DepartmentForm.class))))
             .delete("/{id}", ctx -> {
                 departmentService.delete(ctx.pathId("id"));
                 ctx.noContent();
             });
        }
    }
}
