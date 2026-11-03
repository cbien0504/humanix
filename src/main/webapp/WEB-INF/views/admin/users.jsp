<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Tài khoản người dùng" active="users" script="users">
    <div class="page-header">
        <div>
            <h1 class="h4 mb-1">Tài khoản người dùng</h1>
            <div class="subtitle">Tạo tài khoản, phân quyền theo vai trò, khóa/mở khóa, đặt lại mật khẩu.</div>
        </div>
        <button class="btn btn-primary" id="btnCreate"><i class="bi bi-person-plus me-1"></i>Tạo tài khoản</button>
    </div>

    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <input type="search" class="form-control" name="keyword" placeholder="Tìm username, họ tên, mã NV…" aria-label="Tìm kiếm">
                <select class="form-select" name="role" aria-label="Vai trò">
                    <option value="">Tất cả vai trò</option>
                    <option value="ADMIN">Quản trị hệ thống</option>
                    <option value="HR">Nhân sự</option>
                    <option value="MANAGER">Quản lý</option>
                    <option value="EMPLOYEE">Nhân viên</option>
                </select>
                <select class="form-select" name="active" aria-label="Trạng thái">
                    <option value="">Tất cả trạng thái</option>
                    <option value="true">Đang hoạt động</option>
                    <option value="false">Vô hiệu hóa</option>
                </select>
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0" id="userTable"><thead></thead><tbody></tbody></table>
        </div>
        <div class="data-table-footer"></div>
    </div>

    <div class="modal fade" id="userModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="userForm" novalidate>
                <div class="modal-header">
                    <h5 class="modal-title">Tài khoản</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <input type="hidden" name="userId">
                    <div class="mb-3 create-only">
                        <label class="form-label required" for="fUsername">Tên đăng nhập</label>
                        <input class="form-control" id="fUsername" name="username" maxlength="50" pattern="[a-zA-Z0-9._\-]{2,50}" required>
                        <div class="invalid-feedback">2–50 ký tự: chữ, số, dấu chấm, gạch dưới, gạch ngang.</div>
                    </div>
                    <div class="mb-3 create-only">
                        <label class="form-label" for="fEmployee">Liên kết nhân viên</label>
                        <select class="form-select" id="fEmployee" name="employeeId"></select>
                        <div class="form-text">Tài khoản kỹ thuật có thể không liên kết nhân viên.</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required">Vai trò</label>
                        <div class="d-flex flex-wrap gap-3" id="roleChecks">
                            <div class="form-check"><input class="form-check-input" type="checkbox" value="ADMIN" id="rAdmin"><label class="form-check-label" for="rAdmin">Quản trị</label></div>
                            <div class="form-check"><input class="form-check-input" type="checkbox" value="HR" id="rHr"><label class="form-check-label" for="rHr">Nhân sự</label></div>
                            <div class="form-check"><input class="form-check-input" type="checkbox" value="MANAGER" id="rManager"><label class="form-check-label" for="rManager">Quản lý</label></div>
                            <div class="form-check"><input class="form-check-input" type="checkbox" value="EMPLOYEE" id="rEmployee"><label class="form-check-label" for="rEmployee">Nhân viên</label></div>
                        </div>
                        <div class="text-danger small mt-1 d-none" id="roleError">Chọn ít nhất một vai trò.</div>
                    </div>
                    <div class="form-check form-switch edit-only">
                        <input class="form-check-input" type="checkbox" role="switch" id="fActive" name="active">
                        <label class="form-check-label" for="fActive">Đang hoạt động</label>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>Lưu</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="passwordModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header"><h5 class="modal-title">Mật khẩu tạm thời</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <p class="mb-2">Mật khẩu tạm thời cho <strong id="pwUser"></strong> (chỉ hiển thị một lần):</p>
                    <div class="input-group">
                        <input class="form-control font-monospace" id="pwValue" readonly>
                        <button class="btn btn-outline-secondary" type="button" id="btnCopy"><i class="bi bi-clipboard"></i></button>
                    </div>
                    <p class="small text-body-secondary mt-2 mb-0">Người dùng phải đổi mật khẩu ở lần đăng nhập đầu tiên.</p>
                </div>
            </div>
        </div>
    </div>
</t:layout>
