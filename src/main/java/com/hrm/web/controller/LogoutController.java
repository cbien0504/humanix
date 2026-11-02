package com.hrm.web.controller;

import com.hrm.service.AuditService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** {@code POST /logout} (POST + CSRF token, so a malicious link cannot log users out). */
public class LogoutController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient AuditService audit;

    public LogoutController(AuditService audit) {
        this.audit = audit;
    }

    @Override
    protected void routes(Router r) {
        r.post("/", this::logout);
    }

    private void logout(WebContext ctx) throws IOException {
        HttpSession session = ctx.request().getSession(false);
        if (session != null) {
            if (ctx.user() != null) {
                audit.record(ctx.user().getUsername(), "LOGOUT", "User", ctx.user().getUserId(), null);
            }
            session.invalidate();
        }
        ctx.redirect("/login?logout=1");
    }
}
