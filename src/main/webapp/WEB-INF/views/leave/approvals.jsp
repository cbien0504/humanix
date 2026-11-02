<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Duyệt nghỉ phép" active="leave-approvals" script="leave-approvals">
    <div class="page-header">
        <div>
            <h1 class="h4 mb-1">Duyệt nghỉ phép</h1>
            <div class="subtitle"><c:out value="${u.hr ? 'Đơn của toàn công ty.' : 'Đơn của nhân viên bạn quản lý.'}"/></div>
        </div>
        <c:if test="${u.hr}">
            <button class="btn btn-outline-primary" id="btnInit"><i class="bi bi-calendar-plus me-1"></i>Khởi tạo phép năm</button>
        </c:if>
    </div>
    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <input type="search" class="form-control" name="keyword" placeholder="Mã NV, họ tên…" aria-label="Tìm kiếm">
                <select class="form-select" name="status" aria-label="Trạng thái">
                    <option value="PENDING" selected>Chờ duyệt</option><option value="APPROVED">Đã duyệt</option>
                    <option value="REJECTED">Từ chối</option><option value="CANCELLED">Đã hủy</option><option value="">Tất cả</option>
                </select>
                <select class="form-select" name="leaveTypeId" aria-label="Loại nghỉ">
                    <option value="">Tất cả loại nghỉ</option>
                    <c:forEach items="${leaveTypes}" var="type"><option value="${type.leaveTypeId()}"><c:out value="${type.leaveTypeName()}"/></option></c:forEach>
                </select>
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0" id="approvalTable"><thead></thead><tbody></tbody></table>
        </div>
        <div class="data-table-footer"></div>
    </div>
</t:layout>
