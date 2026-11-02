package com.hrm.tools;

import com.hrm.config.AppConfig;
import com.hrm.config.AppContext;
import com.hrm.exception.AppException;
import com.hrm.model.ImportMode;
import com.hrm.model.ImportResult;
import com.hrm.model.ImportRowError;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.security.UserContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Command-line data migration (UC-72): imports employees from CSV/XLSX with exactly the same
 * validation as the web screen and prints a per-row error report.
 * <pre>
 * java -cp "target/hrm/WEB-INF/classes:target/hrm/WEB-INF/lib/*" com.hrm.tools.DataImportTool \
 *      --file data/import/employees-sample.csv [--mode validate|valid-only|all-or-nothing] [--report errors.csv]
 * </pre>
 * Database settings come from the same HRM_DB_* environment variables as the web application.
 * Exit code: 0 = no row error, 1 = row errors, 2 = usage or fatal error.
 */
public final class DataImportTool {

    private DataImportTool() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Map<String, String> opts = parse(args);
        if (!opts.containsKey("file")) {
            err.println("Cách dùng: DataImportTool --file <employees.csv|.xlsx> [--mode validate|valid-only|all-or-nothing] [--report errors.csv]");
            return 2;
        }
        Path file = Path.of(opts.get("file"));
        if (!Files.isRegularFile(file)) {
            err.println("Không tìm thấy file: " + file);
            return 2;
        }
        ImportMode mode;
        try {
            mode = ImportMode.parse(opts.get("mode"));
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
            return 2;
        }

        try (AppContext app = AppContext.create(AppConfig.load("db.properties", "app.properties"))) {
            UserContext.set(new AuthUser(0, "import-tool", null, "Data import tool", EnumSet.of(Role.HR)));
            ImportResult r;
            try (InputStream in = Files.newInputStream(file)) {
                r = app.importService().importFile(in, file.getFileName().toString(), mode);
            }
            out.printf("File: %s | chế độ: %s%n", r.fileName(), r.mode());
            out.printf("Dòng dữ liệu: %d | hợp lệ: %d | dòng lỗi: %d | đã import: %d%n",
                    r.totalRows(), r.validRows(), r.invalidRows(), r.importedRows());
            r.errors().stream().limit(100).forEach(e ->
                    out.printf("  dòng %-4d %-16s %-24s %s%n", e.row(), e.column(), shorten(e.value()), e.message()));
            if (r.errors().size() > 100) {
                out.printf("  … và %d lỗi khác (xem báo cáo CSV)%n", r.errors().size() - 100);
            }
            if (opts.containsKey("report")) {
                writeReport(Path.of(opts.get("report")), r);
                out.println("Báo cáo lỗi: " + opts.get("report"));
            }
            return r.errors().isEmpty() ? 0 : 1;
        } catch (AppException e) {
            err.println("Lỗi: " + e.getMessage());
            return 2;
        } catch (IOException e) {
            err.println("Không đọc được file: " + e.getMessage());
            return 2;
        } finally {
            UserContext.clear();
        }
    }

    private static void writeReport(Path path, ImportResult r) throws IOException {
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write('﻿');
            w.write("row,column,value,message\r\n");
            for (ImportRowError e : r.errors()) {
                w.write(e.row() + "," + csv(e.column()) + "," + csv(e.value()) + "," + csv(e.message()) + "\r\n");
            }
        }
    }

    private static String csv(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String shorten(String v) {
        if (v == null) {
            return "";
        }
        return v.length() > 22 ? v.substring(0, 21) + "…" : v;
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                m.put(args[i].substring(2), args[++i]);
            }
        }
        return m;
    }
}
