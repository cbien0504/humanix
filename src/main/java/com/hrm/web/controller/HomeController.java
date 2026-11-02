package com.hrm.web.controller;

import com.hrm.web.BaseController;
import com.hrm.web.Router;

/** Context root ("/") – temporary landing page until the dashboard exists. */
public class HomeController extends BaseController {

    private static final long serialVersionUID = 1L;

    @Override
    protected void routes(Router r) {
        r.get("/", ctx -> ctx.render("home"));
    }
}
