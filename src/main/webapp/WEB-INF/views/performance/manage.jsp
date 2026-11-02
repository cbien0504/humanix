<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Đánh giá hiệu suất" active="performance" script="performance-manage">
    <div class="page-header" id="perfPage" data-can-edit="${u.hr}">
        <div><h1 class="h4 mb-1">Đánh giá hiệu suất</h1>
            <div class="subtitle"><c:out value="${u.hr ? 'Quản lý kỳ đánh giá và theo dõi tiến độ toàn công ty.' : 'Phiếu đánh giá của nhân viên bạn quản lý.'}"/></div></div>
        <c:if test="${u.hr}"><button class="btn btn-primary" id="btnNewCycle"><i class="bi bi-plus-lg me-1"></i>Tạo kỳ đánh giá</button></c:if>
    </div>
    <c:if test="${u.hr}">
        <div class="card shadow-sm mb-3">
            <div class="card-header">Kỳ đánh giá</div>
            <div class="table-responsive"><table class="table align-middle mb-0">
                <thead><tr><th>Kỳ</th><th>Giai đoạn</th><th>Hạn</th><th>Trạng thái</th><th style="min-width:180px">Tiến độ</th><th class="text-end">Điểm TB</th><th></th></tr></thead>
                <tbody id="cycles"></tbody></table></div>
        </div>
    </c:if>
    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <select class="form-select" name="cycleId" aria-label="Kỳ đánh giá">
                    <option value="">Tất cả kỳ</option>
                    <c:forEach items="${cycles}" var="cy"><option value="${cy.cycleId}"><c:out value="${cy.cycleName}"/></option></c:forEach>
                </select>
                <select class="form-select" name="status" aria-label="Trạng thái">
                    <option value="">Tất cả trạng thái</option><option value="PENDING_SELF">Chờ tự đánh giá</option>
                    <option value="PENDING_MANAGER" ${u.hr ? '' : 'selected'}>Chờ quản lý chấm</option><option value="COMPLETED">Hoàn thành</option>
                </select>
                <input type="search" class="form-control" name="keyword" placeholder="Mã NV, họ tên…" aria-label="Tìm kiếm">
            </form>
        </div>
        <div class="table-responsive"><table class="table table-hover align-middle mb-0" id="reviewTable"><thead></thead><tbody></tbody></table></div>
        <div class="data-table-footer"></div>
    </div>
    <c:if test="${u.hr}">
    <div class="modal fade" id="cycleModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="cycleForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Tạo kỳ đánh giá</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <div class="row g-3">
                        <div class="col-12"><label class="form-label required" for="cName">Tên kỳ</label>
                            <input class="form-control" id="cName" name="cycleName" required maxlength="150"><div class="invalid-feedback">Nhập tên kỳ.</div></div>
                        <div class="col-6"><label class="form-label required" for="cStart">Từ ngày</label>
                            <input type="date" class="form-control" id="cStart" name="periodStart" required><div class="invalid-feedback">Chọn ngày.</div></div>
                        <div class="col-6"><label class="form-label required" for="cEnd">Đến ngày</label>
                            <input type="date" class="form-control" id="cEnd" name="periodEnd" required data-after="periodStart"><div class="invalid-feedback">Phải sau ngày bắt đầu.</div></div>
                        <div class="col-6"><label class="form-label" for="cDue">Hạn hoàn thành</label><input type="date" class="form-control" id="cDue" name="dueDate"></div>
                        <div class="col-6"><label class="form-label" for="cWeight">Trọng số tự đánh giá</label>
                            <input type="number" class="form-control" id="cWeight" name="selfWeight" min="0" max="1" step="0.05" data-type="number">
                            <div class="invalid-feedback">Từ 0 đến 1.</div></div>
                    </div>
                </div>
                <div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary">Tạo kỳ (Nháp)</button></div>
            </form>
        </div>
    </div>
    </c:if>
</t:layout>
