package com.hrm.web.api;

import com.hrm.security.Role;
import com.hrm.service.UserService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;

import java.io.IOException;

public class UserApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", this::search)
         .get("/eligible-employees", ctx -> ctx.json(userService.employeesWithoutAccount()))
         .post("/", ctx -> ctx.json(201, userService.create(ctx.readJson(UserService.CreateUserCommand.class))))
         .get("/{id}", ctx -> ctx.json(userService.get(ctx.pathId("id"))))
         .put("/{id}", ctx -> ctx.json(userService.update(ctx.requireUser(), ctx.pathId("id"),
                 ctx.readJson(UserService.UpdateUserCommand.class))))
         .post("/{id}/reset-password", ctx -> ctx.json(userService.resetPassword(ctx.pathId("id"))))
         .post("/{id}/unlock", ctx -> ctx.json(userService.unlock(ctx.pathId("id"))));
    }

    private void search(WebContext ctx) throws IOException {
        ctx.json(userService.search(ctx.param("keyword"), ctx.enumParam("role", Role.class),
                ctx.boolParam("active"), ctx.pageRequest(10)));
    }
}
