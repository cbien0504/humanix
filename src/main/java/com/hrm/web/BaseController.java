package com.hrm.web;

import com.hrm.exception.MethodNotAllowedException;
import com.hrm.exception.NotFoundException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public abstract class BaseController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final transient Router router = new Router();

    protected abstract void routes(Router r);

    @Override
    public void init() {
        routes(router);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        Optional<Router.Match> match = router.match(req.getMethod(), path);
        WebContext ctx = new WebContext(req, resp,
                match.map(Router.Match::pathVariables).orElse(Map.of()));
        try {
            if (match.isEmpty()) {
                if (router.matchesOtherMethod(path)) {
                    throw new MethodNotAllowedException(req.getMethod());
                }
                throw new NotFoundException("Không tìm thấy trang hoặc tài nguyên yêu cầu.");
            }
            match.get().handler().handle(ctx);
        } catch (Exception e) {
            ErrorHandler.INSTANCE.handle(ctx, e);
        }
    }
}
