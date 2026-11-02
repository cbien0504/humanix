<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Nghỉ phép của tôi" active="my-leave" script="leave-my">
    <div class="page-header">
        <div><h1 class="h4 mb-1">Nghỉ phép của tôi</h1><div class="subtitle">Số ngày nghỉ = số ngày làm việc (trừ thứ 7, chủ nhật, ngày lễ).</div></div>
        <button class="btn btn-primary" id="btnNew"><i class="bi bi-plus-lg me-1"></i>Tạo đơn nghỉ</button>
    </div>
    <div class="row g-3 mb-3" id="balances"></div>
    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <select class="form-select" name="status" aria-label="Trạng thái">
                    <option value="">Tất cả trạng thái</option>
                    <option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option>
                    <option value="REJECTED">Từ chối</option><option value="CANCELLED">Đã hủy</option>
                </select>
                <input type="number" class="form-control" name="year" id="yearFilter" min="2000" max="2100" aria-label="Năm">
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0" id="leaveTable"><thead></thead><tbody></tbody></table>
        </div>
        <div class="data-table-footer"></div>
    </div>

    <div class="modal fade" id="leaveModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="leaveForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Tạo đơn nghỉ phép</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label required" for="lType">Loại nghỉ</label>
                            <select class="form-select" id="lType" name="leaveTypeId" required data-type="number">
                                <option value="">— Chọn —</option>
                                <c:forEach items="${leaveTypes}" var="type">
                                    <option value="${type.leaveTypeId()}" data-max="${type.maxDaysPerRequest()}"><c:out value="${type.leaveTypeName()}"/><c:if test="${type.paid()}"> (hưởng lương)</c:if></option>
                                </c:forEach>
                            </select>
                            <div class="invalid-feedback">Vui lòng chọn loại nghỉ.</div>
                        </div>
                        <div class="col-6">
                            <label class="form-label required" for="lStart">Từ ngày</label>
                            <input type="date" class="form-control" id="lStart" name="startDate" required>
                            <div class="invalid-feedback">Vui lòng chọn ngày bắt đầu.</div>
                        </div>
                        <div class="col-6">
                            <label class="form-label required" for="lEnd">Đến ngày</label>
                            <input type="date" class="form-control" id="lEnd" name="endDate" required data-after="startDate">
                            <div class="invalid-feedback">Phải từ ngày bắt đầu trở đi.</div>
                        </div>
                        <div class="col-12"><div class="alert alert-info py-2 mb-0 small" id="daysPreview">Chọn ngày để xem số ngày làm việc.</div></div>
                        <div class="col-12">
                            <label class="form-label required" for="lReason">Lý do</label>
                            <textarea class="form-control" id="lReason" name="reason" rows="3" required maxlength="500"></textarea>
                            <div class="invalid-feedback">Vui lòng nhập lý do.</div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-send me-1"></i>Gửi đơn</button>
                </div>
            </form>
        </div>
    </div>
</t:layout>
