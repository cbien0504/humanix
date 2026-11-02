<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Dữ liệu chấm công" active="attendance" script="attendance-manage">
    <div class="page-header" id="attendancePage" data-can-edit="${u.hr}">
        <div>
            <h1 class="h4 mb-1">Dữ liệu chấm công</h1>
            <div class="subtitle"><c:out value="${u.hr ? 'Toàn công ty – HR có thể nhập và điều chỉnh.' : 'Nhân viên trong phạm vi bạn quản lý.'}"/></div>
        </div>
        <c:if test="${u.hr}">
            <button class="btn btn-primary" id="btnAdd"><i class="bi bi-plus-lg me-1"></i>Nhập / điều chỉnh</button>
        </c:if>
    </div>
    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <input type="search" class="form-control" name="keyword" placeholder="Mã NV, họ tên…" aria-label="Tìm kiếm">
                <c:if test="${u.hr}">
                    <select class="form-select" name="departmentId" aria-label="Phòng ban">
                        <option value="">Tất cả phòng ban</option>
                        <c:forEach items="${departments}" var="d"><option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option></c:forEach>
                    </select>
                </c:if>
                <input type="date" class="form-control" name="from" id="fromDate" aria-label="Từ ngày">
                <input type="date" class="form-control" name="to" id="toDate" aria-label="Đến ngày">
                <select class="form-select" name="status" aria-label="Trạng thái">
                    <option value="">Tất cả trạng thái</option>
                    <option value="PRESENT">Đúng giờ</option><option value="LATE">Đi muộn</option>
                    <option value="HALF_DAY">Nửa ngày</option><option value="ABSENT">Vắng</option>
                </select>
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0" id="attendanceTable"><thead></thead><tbody></tbody></table>
        </div>
        <div class="data-table-footer"></div>
    </div>

    <c:if test="${u.hr}">
    <div class="modal fade" id="attModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="attForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Nhập / điều chỉnh chấm công</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label required" for="aEmployee">Nhân viên</label>
                            <select class="form-select" id="aEmployee" name="employeeId" required data-type="number"></select>
                            <div class="invalid-feedback">Vui lòng chọn nhân viên.</div>
                        </div>
                        <div class="col-6">
                            <label class="form-label required" for="aDate">Ngày</label>
                            <input type="date" class="form-control" id="aDate" name="workDate" required>
                            <div class="invalid-feedback">Vui lòng chọn ngày.</div>
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="aStatus">Trạng thái</label>
                            <select class="form-select" id="aStatus" name="status">
                                <option value="">Tự tính theo giờ</option><option value="ABSENT">Vắng</option>
                            </select>
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="aIn">Giờ vào</label>
                            <input type="time" class="form-control" id="aIn" name="checkIn">
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="aOut">Giờ ra</label>
                            <input type="time" class="form-control" id="aOut" name="checkOut">
                            <div class="invalid-feedback">Giờ ra phải sau giờ vào.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="aNote">Ghi chú / lý do điều chỉnh</label>
                            <input class="form-control" id="aNote" name="note" maxlength="500">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>Lưu</button>
                </div>
            </form>
        </div>
    </div>
    </c:if>
</t:layout>
