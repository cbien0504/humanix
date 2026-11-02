<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Chấm công của tôi" active="my-attendance" script="attendance-my">
    <div class="page-header">
        <div><h1 class="h4 mb-1">Chấm công của tôi</h1><div class="subtitle">Giờ chấm công lấy theo đồng hồ máy chủ.</div></div>
    </div>
    <div class="row g-3 mb-3">
        <div class="col-lg-4">
            <div class="card shadow-sm h-100">
                <div class="card-body text-center d-flex flex-column justify-content-center">
                    <div class="text-body-secondary small" id="todayDate">&nbsp;</div>
                    <div class="clock-face my-2" id="clock">--:--:--</div>
                    <div class="mb-3" id="todayStatus"><span class="badge text-bg-light">Đang tải…</span></div>
                    <div class="d-flex justify-content-center gap-3 small mb-3">
                        <div>Vào: <strong id="inTime">--:--</strong></div>
                        <div>Ra: <strong id="outTime">--:--</strong></div>
                    </div>
                    <div class="d-flex justify-content-center gap-2">
                        <button class="btn btn-success px-4" id="btnCheckIn" disabled><i class="bi bi-box-arrow-in-right me-1"></i>Check-in</button>
                        <button class="btn btn-outline-danger px-4" id="btnCheckOut" disabled><i class="bi bi-box-arrow-right me-1"></i>Check-out</button>
                    </div>
                    <div class="small text-body-secondary mt-3" id="schedule"></div>
                </div>
            </div>
        </div>
        <div class="col-lg-8">
            <div class="card shadow-sm h-100">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span>Tổng hợp tháng</span>
                    <input type="month" class="form-control form-control-sm w-auto" id="month" aria-label="Chọn tháng">
                </div>
                <div class="card-body">
                    <div class="row g-3 text-center" id="summary">
                        <div class="col-6 col-md-4"><div class="stat-value" data-k="workedDays">0</div><div class="stat-label">Ngày công</div></div>
                        <div class="col-6 col-md-4"><div class="stat-value text-warning" data-k="lateDays">0</div><div class="stat-label">Lần đi muộn</div></div>
                        <div class="col-6 col-md-4"><div class="stat-value" data-k="totalLateMinutes">0</div><div class="stat-label">Phút đi muộn</div></div>
                        <div class="col-6 col-md-4"><div class="stat-value text-info" data-k="halfDays">0</div><div class="stat-label">Nửa ngày</div></div>
                        <div class="col-6 col-md-4"><div class="stat-value text-success" data-k="paidLeaveDays">0</div><div class="stat-label">Nghỉ có lương</div></div>
                        <div class="col-6 col-md-4"><div class="stat-value" data-k="totalWorkHours">0</div><div class="stat-label">Giờ làm việc</div></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="card shadow-sm">
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead><tr><th>Ngày</th><th>Check-in</th><th>Check-out</th><th class="text-end">Giờ làm</th><th>Trạng thái</th><th class="text-end">Muộn (phút)</th><th>Nguồn</th><th>Ghi chú</th></tr></thead>
                <tbody id="records"></tbody>
            </table>
        </div>
    </div>
</t:layout>
