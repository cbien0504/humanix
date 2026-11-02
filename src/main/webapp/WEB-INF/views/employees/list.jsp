<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Nhân viên" active="employees" script="employees">
    <div class="page-header" id="employeePage" data-can-edit="${u.hr}">
        <div>
            <h1 class="h4 mb-1">Nhân viên</h1>
            <div class="subtitle">
                <c:choose>
                    <c:when test="${u.hr}">Quản lý hồ sơ nhân viên toàn công ty.</c:when>
                    <c:otherwise>Nhân viên thuộc phạm vi bạn quản lý (thông tin lương được ẩn).</c:otherwise>
                </c:choose>
            </div>
        </div>
        <c:if test="${u.hr}">
            <div class="d-flex gap-2">
                <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/import"><i class="bi bi-upload me-1"></i>Import</a>
                <button class="btn btn-primary" id="btnAdd"><i class="bi bi-person-plus me-1"></i>Thêm nhân viên</button>
            </div>
        </c:if>
    </div>

    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <input type="search" class="form-control" name="keyword" placeholder="Mã NV, họ tên, email, SĐT…" aria-label="Tìm kiếm">
                <select class="form-select" name="departmentId" aria-label="Phòng ban">
                    <option value="">Tất cả phòng ban</option>
                    <c:forEach items="${departments}" var="d">
                        <option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option>
                    </c:forEach>
                </select>
                <select class="form-select" name="positionId" aria-label="Chức danh">
                    <option value="">Tất cả chức danh</option>
                    <c:forEach items="${positions}" var="p">
                        <option value="${p.positionId}"><c:out value="${p.positionName}"/></option>
                    </c:forEach>
                </select>
                <select class="form-select" name="status" aria-label="Trạng thái">
                    <option value="">Tất cả trạng thái</option>
                    <c:forEach items="${statuses}" var="s">
                        <option value="${s}"><c:out value="${s.label}"/></option>
                    </c:forEach>
                </select>
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0" id="employeeTable"><thead></thead><tbody></tbody></table>
        </div>
        <div class="data-table-footer"></div>
    </div>

    <c:if test="${u.hr}">
    <div class="modal fade" id="employeeModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-xl modal-dialog-scrollable">
            <form class="modal-content" id="employeeForm" novalidate>
                <div class="modal-header">
                    <h5 class="modal-title">Nhân viên</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <input type="hidden" name="employeeId">
                    <h6 class="text-primary mb-3"><i class="bi bi-person-vcard me-1"></i>Thông tin cá nhân</h6>
                    <div class="row g-3 mb-4">
                        <div class="col-md-3">
                            <label class="form-label required" for="fCode">Mã nhân viên</label>
                            <input class="form-control" id="fCode" name="employeeCode" required maxlength="20" data-rule="code" data-uppercase>
                            <div class="invalid-feedback">Chữ in hoa, số, dấu gạch ngang (3–20 ký tự).</div>
                        </div>
                        <div class="col-md-5">
                            <label class="form-label required" for="fName">Họ và tên</label>
                            <input class="form-control" id="fName" name="fullName" required maxlength="150">
                            <div class="invalid-feedback">Vui lòng nhập họ tên.</div>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label required" for="fGender">Giới tính</label>
                            <select class="form-select" id="fGender" name="gender" required>
                                <option value="M">Nam</option><option value="F">Nữ</option><option value="O">Khác</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label" for="fDob">Ngày sinh</label>
                            <input type="date" class="form-control" id="fDob" name="dateOfBirth">
                            <div class="invalid-feedback">Ngày sinh không hợp lệ.</div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label required" for="fEmail">Email</label>
                            <input type="email" class="form-control" id="fEmail" name="email" required maxlength="150">
                            <div class="invalid-feedback">Email không hợp lệ.</div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label" for="fPhone">Số điện thoại</label>
                            <input class="form-control" id="fPhone" name="phone" maxlength="20" data-rule="phone">
                            <div class="invalid-feedback">Số điện thoại không hợp lệ.</div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label" for="fNationalId">Số CCCD</label>
                            <input class="form-control" id="fNationalId" name="nationalId" maxlength="12" data-rule="nationalId" inputmode="numeric">
                            <div class="invalid-feedback">CCCD gồm đúng 12 chữ số.</div>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label" for="fTax">Mã số thuế</label>
                            <input class="form-control" id="fTax" name="taxCode" maxlength="13" pattern="\d{10}(\d{3})?" inputmode="numeric">
                            <div class="invalid-feedback">10 hoặc 13 chữ số.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="fAddress">Địa chỉ</label>
                            <input class="form-control" id="fAddress" name="address" maxlength="300">
                        </div>
                    </div>
                    <h6 class="text-primary mb-3"><i class="bi bi-briefcase me-1"></i>Công việc</h6>
                    <div class="row g-3 mb-4">
                        <div class="col-md-4">
                            <label class="form-label required" for="fDept">Phòng ban</label>
                            <select class="form-select" id="fDept" name="departmentId" required data-type="number">
                                <option value="">— Chọn —</option>
                                <c:forEach items="${departments}" var="d">
                                    <c:if test="${d.active}"><option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option></c:if>
                                </c:forEach>
                            </select>
                            <div class="invalid-feedback">Vui lòng chọn phòng ban.</div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label required" for="fPosition">Chức danh</label>
                            <select class="form-select" id="fPosition" name="positionId" required data-type="number">
                                <option value="">— Chọn —</option>
                                <c:forEach items="${positions}" var="p">
                                    <c:if test="${p.active}"><option value="${p.positionId}" data-min="${p.minSalary}" data-max="${p.maxSalary}"><c:out value="${p.positionName}"/></option></c:if>
                                </c:forEach>
                            </select>
                            <div class="invalid-feedback">Vui lòng chọn chức danh.</div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="fManager">Quản lý trực tiếp</label>
                            <select class="form-select" id="fManager" name="managerId" data-type="number"></select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label required" for="fHire">Ngày vào làm</label>
                            <input type="date" class="form-control" id="fHire" name="hireDate" required>
                            <div class="invalid-feedback">Vui lòng nhập ngày vào làm.</div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label required" for="fStatus">Trạng thái</label>
                            <select class="form-select" id="fStatus" name="status" required>
                                <c:forEach items="${statuses}" var="s"><option value="${s}"><c:out value="${s.label}"/></option></c:forEach>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label" for="fTerm">Ngày nghỉ việc</label>
                            <input type="date" class="form-control" id="fTerm" name="terminationDate" data-after="hireDate">
                            <div class="invalid-feedback">Bắt buộc khi nghỉ việc; phải sau ngày vào làm.</div>
                        </div>
                    </div>
                    <h6 class="text-primary mb-3"><i class="bi bi-cash-coin me-1"></i>Lương &amp; phúc lợi</h6>
                    <div class="row g-3">
                        <div class="col-md-3">
                            <label class="form-label required" for="fSalary">Lương cơ bản (VND)</label>
                            <input type="number" class="form-control" id="fSalary" name="baseSalary" required min="0" step="1000" data-type="number">
                            <div class="invalid-feedback">Lương không hợp lệ hoặc ngoài khung lương.</div>
                            <div class="form-text" id="salaryBand"></div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label" for="fAllowance">Phụ cấp (VND)</label>
                            <input type="number" class="form-control" id="fAllowance" name="allowance" min="0" step="1000" data-type="number">
                            <div class="invalid-feedback">Phụ cấp không được âm.</div>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label" for="fDependents">Người phụ thuộc</label>
                            <input type="number" class="form-control" id="fDependents" name="dependents" min="0" max="20" step="1" data-type="number">
                            <div class="invalid-feedback">Từ 0 đến 20.</div>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label" for="fBank">Ngân hàng</label>
                            <input class="form-control" id="fBank" name="bankName" maxlength="100">
                        </div>
                        <div class="col-md-2">
                            <label class="form-label" for="fAccount">Số tài khoản</label>
                            <input class="form-control" id="fAccount" name="bankAccount" maxlength="30">
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
