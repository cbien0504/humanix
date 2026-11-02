<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="http://hrm.example.com/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="u" value="${sessionScope.authUser}"/>
<c:set var="e" value="${employee}"/>
<t:layout title="${e.fullName}" active="${self ? 'profile' : 'employees'}">
    <div class="page-header">
        <div class="d-flex align-items-center gap-3">
            <div class="stat-icon bg-soft-primary rounded-circle" style="width:56px;height:56px;font-size:1.5rem">
                <c:out value="${e.fullName.substring(0, 1)}"/>
            </div>
            <div>
                <h1 class="h4 mb-1"><c:out value="${e.fullName}"/></h1>
                <div class="subtitle">
                    <span class="fw-semibold"><c:out value="${e.employeeCode}"/></span> ·
                    <c:out value="${e.positionName}"/> · <c:out value="${e.departmentName}"/>
                    <span class="badge ms-1 ${e.status == 'ACTIVE' ? 'text-bg-success' : e.status == 'PROBATION' ? 'text-bg-warning' : e.status == 'ON_LEAVE' ? 'text-bg-info' : 'text-bg-secondary'}">
                        <c:out value="${e.status.label}"/>
                    </span>
                </div>
            </div>
        </div>
        <div class="d-flex gap-2">
            <c:if test="${not self}">
                <a class="btn btn-light" href="${ctx}/employees"><i class="bi bi-arrow-left me-1"></i>Danh sách</a>
            </c:if>
            <c:if test="${u.hr}">
                <a class="btn btn-primary" href="${ctx}/employees?edit=${e.employeeId}"><i class="bi bi-pencil me-1"></i>Chỉnh sửa</a>
            </c:if>
        </div>
    </div>

    <div class="row g-3">
        <div class="col-lg-6">
            <div class="card shadow-sm h-100">
                <div class="card-header"><i class="bi bi-person-vcard me-1"></i>Thông tin cá nhân</div>
                <div class="card-body">
                    <dl class="row mb-0">
                        <dt class="col-sm-4">Giới tính</dt><dd class="col-sm-8"><c:out value="${e.genderLabel}"/></dd>
                        <dt class="col-sm-4">Ngày sinh</dt>
                        <dd class="col-sm-8"><c:out value="${h:date(e.dateOfBirth)}"/><c:if test="${not empty e.age}"> (<c:out value="${e.age}"/> tuổi)</c:if></dd>
                        <dt class="col-sm-4">Email</dt><dd class="col-sm-8"><c:out value="${e.email}"/></dd>
                        <dt class="col-sm-4">Điện thoại</dt><dd class="col-sm-8"><c:out value="${e.phone}"/></dd>
                        <dt class="col-sm-4">Địa chỉ</dt><dd class="col-sm-8"><c:out value="${e.address}"/></dd>
                        <c:if test="${not empty e.nationalId}">
                            <dt class="col-sm-4">Số CCCD</dt><dd class="col-sm-8"><c:out value="${e.nationalId}"/></dd>
                        </c:if>
                        <c:if test="${not empty e.taxCode}">
                            <dt class="col-sm-4">Mã số thuế</dt><dd class="col-sm-8"><c:out value="${e.taxCode}"/></dd>
                        </c:if>
                    </dl>
                </div>
            </div>
        </div>
        <div class="col-lg-6">
            <div class="card shadow-sm h-100">
                <div class="card-header"><i class="bi bi-briefcase me-1"></i>Công việc</div>
                <div class="card-body">
                    <dl class="row mb-0">
                        <dt class="col-sm-4">Phòng ban</dt><dd class="col-sm-8"><c:out value="${e.departmentName}"/></dd>
                        <dt class="col-sm-4">Chức danh</dt><dd class="col-sm-8"><c:out value="${e.positionName}"/></dd>
                        <dt class="col-sm-4">Quản lý trực tiếp</dt>
                        <dd class="col-sm-8"><c:out value="${empty e.managerName ? '—' : e.managerCode.concat(' – ').concat(e.managerName)}"/></dd>
                        <dt class="col-sm-4">Ngày vào làm</dt><dd class="col-sm-8"><c:out value="${h:date(e.hireDate)}"/></dd>
                        <dt class="col-sm-4">Thâm niên</dt><dd class="col-sm-8"><c:out value="${h:number(e.yearsOfService)}"/> năm</dd>
                        <c:if test="${not empty e.terminationDate}">
                            <dt class="col-sm-4">Ngày nghỉ việc</dt><dd class="col-sm-8"><c:out value="${h:date(e.terminationDate)}"/></dd>
                        </c:if>
                        <dt class="col-sm-4">Tài khoản</dt>
                        <dd class="col-sm-8">
                            <c:choose>
                                <c:when test="${empty e.username}">Chưa có</c:when>
                                <c:otherwise><c:out value="${e.username}"/>
                                    <c:if test="${not e.hasActiveAccount}"><span class="badge text-bg-secondary ms-1">Vô hiệu hóa</span></c:if>
                                </c:otherwise>
                            </c:choose>
                        </dd>
                    </dl>
                </div>
            </div>
        </div>

        <c:if test="${not empty e.baseSalary}">
            <div class="col-lg-6">
                <div class="card shadow-sm h-100">
                    <div class="card-header"><i class="bi bi-cash-coin me-1"></i>Lương &amp; phúc lợi</div>
                    <div class="card-body">
                        <dl class="row mb-0">
                            <dt class="col-sm-5">Lương cơ bản</dt><dd class="col-sm-7"><c:out value="${h:money(e.baseSalary)}"/> ₫</dd>
                            <dt class="col-sm-5">Phụ cấp</dt><dd class="col-sm-7"><c:out value="${h:money(e.allowance)}"/> ₫</dd>
                            <dt class="col-sm-5">Người phụ thuộc</dt><dd class="col-sm-7"><c:out value="${e.dependents}"/></dd>
                            <dt class="col-sm-5">Ngân hàng</dt><dd class="col-sm-7"><c:out value="${e.bankName}"/> <c:out value="${e.bankAccount}"/></dd>
                        </dl>
                    </div>
                </div>
            </div>
        </c:if>

        <c:if test="${not empty salaryHistory}">
            <div class="col-lg-6">
                <div class="card shadow-sm h-100">
                    <div class="card-header"><i class="bi bi-clock-history me-1"></i>Lịch sử lương</div>
                    <div class="table-responsive">
                        <table class="table table-sm mb-0">
                            <thead><tr><th>Thời điểm</th><th class="text-end">Lương CB</th><th class="text-end">Phụ cấp</th><th>Người sửa</th></tr></thead>
                            <tbody>
                            <c:forEach items="${salaryHistory}" var="s">
                                <tr>
                                    <td><c:out value="${h:instant(s.changedAt())}"/></td>
                                    <td class="money">
                                        <c:if test="${not empty s.oldBaseSalary()}"><span class="text-body-secondary"><c:out value="${h:money(s.oldBaseSalary())}"/> →</span></c:if>
                                        <c:out value="${h:money(s.newBaseSalary())}"/>
                                    </td>
                                    <td class="money"><c:out value="${h:money(s.newAllowance())}"/></td>
                                    <td><c:out value="${s.changedBy()}"/></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </c:if>
    </div>
</t:layout>
