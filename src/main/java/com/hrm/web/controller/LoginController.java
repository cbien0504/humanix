package com.hrm.web.controller;

import com.hrm.exception.UnauthorizedException;
import com.hrm.security.CsrfTokens;
import com.hrm.service.AuthService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;
import com.hrm.web.filter.AuthenticationFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class LoginController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient AuthService authService;
    private final boolean showDemoAccounts;

    public LoginController(AuthService authService, boolean showDemoAccounts) {
        this.authService = authService;
        this.showDemoAccounts = showDemoAccounts;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", this::form).post("/", this::login);
    }

    private void form(WebContext ctx) throws ServletException, IOException {
        if (ctx.user() != null) {
            ctx.redirect("/");
            return;
        }
        ctx.attr("next", safeNext(ctx.param("next"))).attr("showDemoAccounts", showDemoAccounts).render("login");
    }

    private void login(WebContext ctx) throws ServletException, IOException {
        String username = ctx.param("username");
        String next = safeNext(ctx.param("next"));
        try {
            AuthService.LoginResult result = authService.login(username, ctx.request().getParameter("password"));
            HttpServletRequest req = ctx.request();
            HttpSession session = req.getSession();
            req.changeSessionId();
            session.setAttribute(WebContext.SESSION_USER, result.user());
            CsrfTokens.rotate(session);
            if (result.mustChangePassword()) {
                session.setAttribute(AuthenticationFilter.MUST_CHANGE_PASSWORD, Boolean.TRUE);
                ctx.redirect("/profile/password?required=1");
                return;
            }
            ctx.redirect(next);
        } catch (UnauthorizedException e) {
            ctx.attr("error", e.getMessage()).attr("username", username).attr("next", next)
                    .attr("showDemoAccounts", showDemoAccounts).render("login");
        }
    }

    static String safeNext(String next) {
        if (next == null || !next.startsWith("/") || next.startsWith("//") || next.contains("\\")
                || next.contains("://") || next.startsWith("/login")) {
            return "/";
        }
        return next;
    }
}
