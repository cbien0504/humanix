<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Kiểm tra toàn vẹn dữ liệu" active="data-quality" script="data-quality">
    <div class="page-header">
        <div>
            <h1 class="h4 mb-1">Kiểm tra toàn vẹn dữ liệu</h1>
            <div class="subtitle">Chạy <code>sp_RunDataValidation</code>: trùng mã NV, lương âm, nghỉ phép chồng lấn, số dư phép lệch…</div>
        </div>
        <button class="btn btn-primary" id="btnRun"><i class="bi bi-play-circle me-1"></i>Chạy kiểm tra</button>
    </div>
    <div class="row g-3 mb-3" id="summary" hidden>
        <div class="col-sm-4"><div class="card stat-card"><div class="card-body d-flex gap-3 align-items-center">
            <div class="stat-icon bg-soft-success"><i class="bi bi-check2-circle"></i></div>
            <div><div class="stat-value" id="cntPass">0</div><div class="stat-label">Đạt</div></div></div></div></div>
        <div class="col-sm-4"><div class="card stat-card"><div class="card-body d-flex gap-3 align-items-center">
            <div class="stat-icon bg-soft-warning"><i class="bi bi-exclamation-triangle"></i></div>
            <div><div class="stat-value" id="cntWarn">0</div><div class="stat-label">Cảnh báo</div></div></div></div></div>
        <div class="col-sm-4"><div class="card stat-card"><div class="card-body d-flex gap-3 align-items-center">
            <div class="stat-icon bg-soft-danger"><i class="bi bi-x-octagon"></i></div>
            <div><div class="stat-value" id="cntFail">0</div><div class="stat-label">Lỗi</div></div></div></div></div>
    </div>
    <div class="card shadow-sm">
        <div class="table-responsive">
            <table class="table align-middle mb-0">
                <thead><tr><th>Mã</th><th>Quy tắc</th><th>Mức độ</th><th class="text-end">Vi phạm</th><th>Kết quả</th><th>Ví dụ</th></tr></thead>
                <tbody id="checks"><tr><td colspan="6" class="empty-state">Bấm "Chạy kiểm tra" để bắt đầu.</td></tr></tbody>
            </table>
        </div>
    </div>
</t:layout>
