package com.hrm.web.controller;

import com.hrm.exception.ValidationException;
import com.hrm.model.ImportMode;
import com.hrm.service.EmployeeImportService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;
import com.hrm.web.WebContext;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ImportController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient EmployeeImportService importService;

    public ImportController(EmployeeImportService importService) {
        this.importService = importService;
    }

    @Override
    protected void routes(Router r) {
        r.get("/", ctx -> ctx.render("import/index"))
         .get("/template.csv", this::template)
         .post("/employees", this::upload);
    }

    private void template(WebContext ctx) throws IOException {
        try (OutputStream out = ctx.download("mau-import-nhan-vien.csv", "text/csv;charset=UTF-8")) {
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            out.write(EmployeeImportService.template().getBytes(StandardCharsets.UTF_8));
        }
    }

    private void upload(WebContext ctx) throws Exception {
        Part part;
        try {
            part = ctx.request().getPart("file");
        } catch (IllegalStateException e) {
            throw new ValidationException("file", "File vượt quá dung lượng cho phép (5 MB).");
        }
        if (part == null || part.getSize() == 0) {
            throw new ValidationException("file", "Vui lòng chọn file .csv hoặc .xlsx.");
        }
        ImportMode mode;
        try {
            mode = ImportMode.parse(ctx.param("mode"));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("mode", e.getMessage());
        }
        try (InputStream in = part.getInputStream()) {
            ctx.json(importService.importFile(in, part.getSubmittedFileName(), mode));
        }
    }
}
