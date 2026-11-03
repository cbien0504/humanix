package com.hrm.dao.jdbc;

import com.hrm.exception.BusinessException;
import com.hrm.exception.DataAccessException;
import com.hrm.exception.DataIntegrityException;
import com.hrm.exception.DatabaseUnavailableException;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlErrorTranslator {

    private static final Pattern CONSTRAINT_NAME =
            Pattern.compile("(?:constraint|index) ['\"]([A-Za-z0-9_]+)['\"]", Pattern.CASE_INSENSITIVE);

    private record Hint(String field, String message) {
    }

    private static final Map<String, Hint> HINTS = Map.ofEntries(
            Map.entry("UQ_Employees_EmployeeCode", new Hint("employeeCode", "Mã nhân viên đã tồn tại.")),
            Map.entry("UQ_Employees_Email", new Hint("email", "Email đã được dùng cho một nhân viên khác.")),
            Map.entry("UX_Employees_NationalId", new Hint("nationalId", "Số CCCD đã tồn tại.")),
            Map.entry("CK_Employees_Code", new Hint("employeeCode", "Mã nhân viên chỉ gồm chữ in hoa, chữ số, dấu gạch ngang (tối thiểu 3 ký tự).")),
            Map.entry("CK_Employees_Salary", new Hint("baseSalary", "Lương cơ bản và phụ cấp không được âm.")),
            Map.entry("CK_Employees_MinAge", new Hint("dateOfBirth", "Nhân viên phải đủ 15 tuổi tại ngày vào làm.")),
            Map.entry("CK_Employees_TermDate", new Hint("terminationDate", "Ngày nghỉ việc phải sau ngày vào làm.")),
            Map.entry("CK_Employees_Terminated", new Hint("terminationDate", "Nhân viên nghỉ việc phải có ngày nghỉ việc.")),
            Map.entry("CK_Employees_SelfManager", new Hint("managerId", "Nhân viên không thể là quản lý của chính mình.")),
            Map.entry("UQ_Departments_Code", new Hint("departmentCode", "Mã phòng ban đã tồn tại.")),
            Map.entry("UQ_Departments_Name", new Hint("departmentName", "Tên phòng ban đã tồn tại.")),
            Map.entry("UQ_Positions_Code", new Hint("positionCode", "Mã chức danh đã tồn tại.")),
            Map.entry("CK_Positions_SalaryRange", new Hint("maxSalary", "Lương tối đa phải lớn hơn hoặc bằng lương tối thiểu.")),
            Map.entry("UQ_Users_Username", new Hint("username", "Tên đăng nhập đã tồn tại.")),
            Map.entry("UX_Users_EmployeeId", new Hint("employeeId", "Nhân viên này đã có tài khoản.")),
            Map.entry("UQ_Attendance_EmployeeDate", new Hint("workDate", "Nhân viên đã có bản ghi chấm công trong ngày này.")),
            Map.entry("CK_Attendance_CheckOrder", new Hint("checkOut", "Giờ check-out phải sau giờ check-in.")),
            Map.entry("CK_Attendance_SameDay", new Hint("checkIn", "Giờ check-in phải thuộc ngày chấm công.")),
            Map.entry("CK_LeaveBalances_Days", new Hint(null, "Số ngày phép đã duyệt vượt quá số ngày phép được hưởng.")),
            Map.entry("CK_LeaveRequests_Dates", new Hint("endDate", "Ngày kết thúc phải từ ngày bắt đầu trở đi và trong cùng một năm.")),
            Map.entry("UQ_PayrollPeriods_YearMonth", new Hint(null, "Kỳ lương tháng này đã tồn tại.")),
            Map.entry("UQ_JobPostings_JobCode", new Hint("jobCode", "Mã tin tuyển dụng đã tồn tại.")),
            Map.entry("UQ_Candidates_JobEmail", new Hint("email", "Ứng viên với email này đã nộp hồ sơ cho tin tuyển dụng.")),
            Map.entry("UQ_ReviewCycles_Name", new Hint("cycleName", "Tên kỳ đánh giá đã tồn tại.")),
            Map.entry("FK_Employees_Departments", new Hint(null, "Phòng ban đang có nhân viên nên không thể xóa.")),
            Map.entry("FK_Employees_Positions", new Hint(null, "Chức danh đang được gán cho nhân viên nên không thể xóa.")),
            Map.entry("FK_JobPostings_Departments", new Hint(null, "Phòng ban đang có tin tuyển dụng nên không thể xóa.")),
            Map.entry("FK_JobPostings_Positions", new Hint(null, "Chức danh đang có tin tuyển dụng nên không thể xóa.")),
            Map.entry("FK_Employees_Manager", new Hint(null, "Nhân viên đang là quản lý trực tiếp của người khác.")),
            Map.entry("FK_Departments_Manager", new Hint(null, "Nhân viên đang là trưởng phòng.")),
            Map.entry("FK_Attendance_Employees", new Hint(null, "Nhân viên đã có dữ liệu chấm công – hãy chuyển sang trạng thái Nghỉ việc thay vì xóa.")),
            Map.entry("FK_LeaveRequests_Employees", new Hint(null, "Nhân viên đã có đơn nghỉ phép – hãy chuyển sang trạng thái Nghỉ việc thay vì xóa.")),
            Map.entry("FK_Payroll_Employees", new Hint(null, "Nhân viên đã có dữ liệu lương – hãy chuyển sang trạng thái Nghỉ việc thay vì xóa.")),
            Map.entry("FK_PerformanceReviews_Employees", new Hint(null, "Nhân viên đã có phiếu đánh giá – hãy chuyển sang trạng thái Nghỉ việc thay vì xóa.")),
            Map.entry("FK_Candidates_JobPostings", new Hint(null, "Tin tuyển dụng đã có ứng viên nên không thể xóa.")));

    public RuntimeException translate(SQLException e, String sql) {
        if (isConnectionFailure(e)) {
            return new DatabaseUnavailableException(e);
        }
        int code = e.getErrorCode();
        if (code == 2627 || code == 2601 || code == 547) {
            String constraint = constraintName(e.getMessage());
            Hint hint = constraint == null ? null : HINTS.get(constraint);
            String message = hint != null ? hint.message()
                    : (code == 547 ? "Dữ liệu vi phạm ràng buộc toàn vẹn" : "Dữ liệu bị trùng lặp")
                      + (constraint != null ? " (" + constraint + ")." : ".");
            return new DataIntegrityException(message, hint == null ? null : hint.field(), constraint, e);
        }
        if (code >= 51000 && code <= 51999) {
            return new BusinessException(e.getMessage(), e);
        }
        if (code == 1205) {
            return new DataAccessException("Hệ thống đang bận do xung đột dữ liệu, vui lòng thử lại.", e);
        }
        return new DataAccessException("Lỗi truy cập cơ sở dữ liệu [" + code + "] khi thực thi: " + abbreviate(sql), e);
    }

    static String constraintName(String message) {
        if (message == null) {
            return null;
        }
        Matcher m = CONSTRAINT_NAME.matcher(message);
        return m.find() ? m.group(1) : null;
    }

    private static boolean isConnectionFailure(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLTransientConnectionException) {
                return true;
            }
            if (t instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) {
                return true;
            }
        }
        return false;
    }

    private static String abbreviate(String sql) {
        if (sql == null) {
            return "";
        }
        String flat = sql.replaceAll("\\s+", " ").trim();
        return flat.length() > 120 ? flat.substring(0, 120) + "…" : flat;
    }
}
