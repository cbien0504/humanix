package com.hrm.web.controller;

import com.hrm.service.AuthService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;
import com.hrm.web.filter.AuthenticationFilter;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/** {@code /profile/*}: self-service pages of the signed-in user. */
public class ProfileController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    /** JSON body of the change-password form. */
    public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
    }

    @Override
    protected void routes(Router r) {
        r.get("/password", ctx -> ctx.render("profile/password"))
         .post("/password", this::changePassword);
    }

    private void changePassword(WebContext ctx) throws IOException {
        ChangePasswordRequest body = ctx.readJson(ChangePasswordRequest.class);
        authService.changePassword(ctx.requireUser(), body.currentPassword(), body.newPassword(), body.confirmPassword());
        HttpSession session = ctx.request().getSession(false);
        if (session != null) {
            session.removeAttribute(AuthenticationFilter.MUST_CHANGE_PASSWORD);
        }
        ctx.json(Map.of("message", "Đổi mật khẩu thành công."));
    }
}
