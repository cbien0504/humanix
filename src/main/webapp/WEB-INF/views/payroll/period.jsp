<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="http://hrm.example.com/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Bảng lương ${period.label}" active="payroll" script="payroll-period">
    <div class="page-header" id="periodPage" data-period-id="${period.periodId}" data-status="${period.status}">
        <div>
            <h1 class="h4 mb-1">Bảng lương tháng <c:out value="${period.label}"/></h1>
            <div class="subtitle">
                <span class="badge text-bg-${period.status == 'PAID' ? 'success' : period.status == 'APPROVED' ? 'primary' : 'info'}"><c:out value="${period.status.label}"/></span>
                · Công chuẩn <c:out value="${h:number(period.standardWorkDays)}"/> ngày
                · <c:out value="${period.employeeCount}"/> nhân viên
                · Tính lúc <c:out value="${h:instant(period.calculatedAt)}"/> bởi <c:out value="${period.calculatedBy}"/>
                <c:if test="${not empty period.approvedBy}"> · Duyệt bởi <c:out value="${period.approvedBy}"/></c:if>
            </div>
        </div>
        <div class="d-flex gap-2 flex-wrap">
            <a class="btn btn-light" href="${ctx}/payroll"><i class="bi bi-arrow-left me-1"></i>Kỳ lương</a>
            <a class="btn btn-outline-success" href="${ctx}/payroll/${period.periodId}/export?format=xlsx"><i class="bi bi-file-earmark-excel me-1"></i>Excel</a>
            <a class="btn btn-outline-secondary" href="${ctx}/payroll/${period.periodId}/export?format=csv"><i class="bi bi-filetype-csv me-1"></i>CSV</a>
        </div>
    </div>
    <div class="row g-3 mb-3">
        <div class="col-md-3"><div class="card stat-card"><div class="card-body"><div class="stat-label">Tổng thu nhập (gross)</div>
            <div class="stat-value"><c:out value="${h:money(period.totalGross)}"/></div></div></div></div>
        <div class="col-md-3"><div class="card stat-card"><div class="card-body"><div class="stat-label">Tổng thực lĩnh (net)</div>
            <div class="stat-value text-success"><c:out value="${h:money(period.totalNet)}"/></div></div></div></div>
        <div class="col-md-6"><div class="card h-100"><div class="card-body py-2">
            <div class="stat-label mb-1">Theo phòng ban</div>
            <div class="table-responsive"><table class="table table-sm mb-0"><tbody id="deptTotals"></tbody></table></div>
        </div></div></div>
    </div>
    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <input type="search" class="form-control" name="keyword" placeholder="Mã NV, họ tên…" aria-label="Tìm kiếm">
                <select class="form-select" name="departmentId" aria-label="Phòng ban">
                    <option value="">Tất cả phòng ban</option>
                    <c:forEach items="${departments}" var="d"><option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option></c:forEach>
                </select>
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0" id="payslipTable"><thead></thead><tbody></tbody></table>
        </div>
        <div class="data-table-footer"></div>
    </div>
</t:layout>
