<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Nhật ký hệ thống" active="audit" script="audit">
    <div class="page-header">
        <div>
            <h1 class="h4 mb-1">Nhật ký hệ thống</h1>
            <div class="subtitle">Đăng nhập, thay đổi dữ liệu quan trọng, duyệt lương… (thời gian theo giờ máy bạn).</div>
        </div>
    </div>
    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <input type="search" class="form-control" name="keyword" placeholder="Người dùng, nội dung…" aria-label="Tìm kiếm">
                <select class="form-select" name="action" aria-label="Hành động">
                    <option value="">Tất cả hành động</option>
                    <option>LOGIN_SUCCESS</option><option>LOGIN_FAILED</option><option>ACCOUNT_LOCKED</option><option>LOGOUT</option>
                    <option>PASSWORD_CHANGED</option><option>PASSWORD_RESET</option><option>USER_CREATED</option><option>USER_UPDATED</option>
                    <option>EMPLOYEE_CREATED</option><option>EMPLOYEE_UPDATED</option><option>EMPLOYEE_DELETED</option>
                    <option>LEAVE_APPROVED</option><option>LEAVE_REJECTED</option>
                    <option>PAYROLL_CALCULATED</option><option>PAYROLL_APPROVED</option><option>PAYROLL_PAID</option>
                    <option>DATA_IMPORTED</option><option>DATA_VALIDATION_RUN</option>
                </select>
                <input type="date" class="form-control" name="from" aria-label="Từ ngày">
                <input type="date" class="form-control" name="to" aria-label="Đến ngày">
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0" id="auditTable"><thead></thead><tbody></tbody></table>
        </div>
        <div class="data-table-footer"></div>
    </div>
</t:layout>
