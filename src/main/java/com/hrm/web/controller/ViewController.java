package com.hrm.web.controller;

import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class ViewController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final String view;

    public ViewController(String view) {
        this.view = view;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", ctx -> ctx.render(view));
    }
}
