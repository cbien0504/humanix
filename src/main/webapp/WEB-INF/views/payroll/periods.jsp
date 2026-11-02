<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Bảng lương" active="payroll" script="payroll-periods">
    <div class="page-header">
        <div><h1 class="h4 mb-1">Bảng lương</h1>
            <div class="subtitle">Quy trình: Tính lương (stored procedure) → Duyệt (khóa số liệu) → Đánh dấu đã chi trả.</div></div>
    </div>
    <div class="card shadow-sm mb-3">
        <div class="card-body">
            <form class="row g-2 align-items-end" id="calcForm" novalidate>
                <div class="col-auto">
                    <label class="form-label" for="calcMonth">Tháng cần tính</label>
                    <input type="month" class="form-control" id="calcMonth" required>
                </div>
                <div class="col-auto">
                    <button class="btn btn-primary" type="submit"><i class="bi bi-calculator me-1"></i>Tính lương</button>
                </div>
                <div class="col small text-body-secondary">
                    Gross = lương CB và phụ cấp theo ngày công; BHXH 8%, BHYT 1,5%, BHTN 1% (có trần); thuế TNCN lũy tiến sau giảm trừ gia cảnh.
                    Tham số: <a href="${pageContext.request.contextPath}/admin/settings">cấu hình lương</a>.
                </div>
            </form>
        </div>
    </div>
    <div class="card shadow-sm">
        <div class="card-header d-flex justify-content-between align-items-center">
            <span>Các kỳ lương</span>
            <select class="form-select form-select-sm w-auto" id="yearFilter" aria-label="Năm"></select>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead><tr><th>Kỳ</th><th class="text-end">Công chuẩn</th><th class="text-end">Số NV</th><th class="text-end">Tổng thu nhập</th>
                    <th class="text-end">Tổng thực lĩnh</th><th>Trạng thái</th><th>Tính / duyệt</th><th></th></tr></thead>
                <tbody id="periods"></tbody>
            </table>
        </div>
    </div>
</t:layout>
