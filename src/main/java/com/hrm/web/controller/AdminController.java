package com.hrm.web.controller;

import com.hrm.web.BaseController;
import com.hrm.web.Router;

/** {@code /admin/*}: administration pages (data is loaded by AJAX from /api/...). */
public class AdminController extends BaseController {

    private static final long serialVersionUID = 1L;

    @Override
    protected void routes(Router r) {
        r.get("/users", ctx -> ctx.render("admin/users"))
         .get("/audit", ctx -> ctx.render("admin/audit"))
         .get("/data-quality", ctx -> ctx.render("admin/data-quality"))
         .get("/settings", ctx -> ctx.render("admin/settings"));
    }
}
