<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="http://hrm.example.com/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="${job.title}" active="recruitment" script="recruitment-pipeline">
    <div class="page-header" id="pipelinePage" data-job-id="${job.jobPostingId}" data-can-edit="${u.hr}" data-job-status="${job.status}">
        <div>
            <h1 class="h4 mb-1"><c:out value="${job.title}"/></h1>
            <div class="subtitle"><span class="fw-semibold"><c:out value="${job.jobCode}"/></span> ·
                <c:out value="${job.departmentName}"/> · <c:out value="${job.positionName}"/> ·
                Đã tuyển <span id="hiredCount"><c:out value="${job.hiredCount}"/></span>/<c:out value="${job.openings}"/> ·
                <c:out value="${job.status.label}"/>
                <c:if test="${not empty job.closeDate}"> · Hạn: <c:out value="${h:date(job.closeDate)}"/></c:if></div>
        </div>
        <div class="d-flex gap-2">
            <a class="btn btn-light" href="${pageContext.request.contextPath}/recruitment"><i class="bi bi-arrow-left me-1"></i>Tin tuyển dụng</a>
            <c:if test="${u.hr and job.status == 'OPEN'}"><button class="btn btn-primary" id="btnAddCandidate"><i class="bi bi-person-plus me-1"></i>Thêm ứng viên</button></c:if>
        </div>
    </div>
    <div class="pipeline" id="board"></div>

    <div class="modal fade" id="candidateModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header"><h5 class="modal-title" id="cTitle"></h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="row g-3">
                        <div class="col-md-6">
                            <dl class="row small mb-0" id="cInfo"></dl>
                            <h6 class="mt-3">Lịch sử pipeline</h6>
                            <ul class="list-unstyled small mb-0" id="cHistory"></ul>
                        </div>
                        <div class="col-md-6">
                            <form id="reviewForm" novalidate>
                                <div class="alert alert-danger d-none form-alert"></div>
                                <div class="mb-2"><label class="form-label" for="rRating">Đánh giá (1–5)</label>
                                    <select class="form-select" id="rRating" name="rating" data-type="number"><option value="">—</option>
                                        <option>1</option><option>2</option><option>3</option><option>4</option><option>5</option></select></div>
                                <div class="mb-2"><label class="form-label" for="rInterview">Lịch phỏng vấn</label>
                                    <input type="datetime-local" class="form-control" id="rInterview" name="interviewAt"></div>
                                <div class="mb-2"><label class="form-label" for="rNotes">Nhận xét</label>
                                    <textarea class="form-control" id="rNotes" name="notes" rows="4" maxlength="2000"></textarea></div>
                                <button class="btn btn-outline-primary btn-sm" type="submit"><i class="bi bi-save me-1"></i>Lưu nhận xét</button>
                            </form>
                        </div>
                    </div>
                </div>
                <div class="modal-footer justify-content-between" id="stageActions"></div>
            </div>
        </div>
    </div>

    <c:if test="${u.hr}">
    <div class="modal fade" id="addModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="addForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Thêm ứng viên</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <div class="row g-3">
                        <div class="col-12"><label class="form-label required" for="aName">Họ tên</label>
                            <input class="form-control" id="aName" name="fullName" required maxlength="150"><div class="invalid-feedback">Nhập họ tên.</div></div>
                        <div class="col-md-7"><label class="form-label required" for="aEmail">Email</label>
                            <input type="email" class="form-control" id="aEmail" name="email" required maxlength="150"><div class="invalid-feedback">Email không hợp lệ.</div></div>
                        <div class="col-md-5"><label class="form-label" for="aPhone">Điện thoại</label>
                            <input class="form-control" id="aPhone" name="phone" data-rule="phone"><div class="invalid-feedback">SĐT không hợp lệ.</div></div>
                        <div class="col-md-6"><label class="form-label" for="aSource">Nguồn</label>
                            <select class="form-select" id="aSource" name="source"><option>WEBSITE</option><option>LINKEDIN</option><option>TOPCV</option>
                                <option>VIETNAMWORKS</option><option>REFERRAL</option><option>OTHER</option></select></div>
                        <div class="col-md-6"><label class="form-label" for="aSalary">Lương mong muốn</label>
                            <input type="number" class="form-control" id="aSalary" name="expectedSalary" min="0" step="500000" data-type="number"></div>
                        <div class="col-12"><label class="form-label" for="aCv">Link CV</label>
                            <input type="url" class="form-control" id="aCv" name="resumeUrl" placeholder="https://..." pattern="https?://.+">
                            <div class="invalid-feedback">Link phải bắt đầu bằng http(s)://</div></div>
                        <div class="col-12"><label class="form-label" for="aNotes">Ghi chú</label><textarea class="form-control" id="aNotes" name="notes" rows="2"></textarea></div>
                    </div>
                </div>
                <div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary">Thêm</button></div>
            </form>
        </div>
    </div>
    <div class="modal fade" id="hireModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="hireForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Tiếp nhận ứng viên thành nhân viên</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <p class="small text-body-secondary">Tạo hồ sơ nhân viên <strong>thử việc</strong> thuộc phòng ban/chức danh của tin tuyển dụng.</p>
                    <div class="row g-3">
                        <div class="col-md-6"><label class="form-label" for="hCode">Mã nhân viên</label>
                            <input class="form-control" id="hCode" name="employeeCode" data-rule="code" data-uppercase><div class="invalid-feedback">Mã không hợp lệ.</div></div>
                        <div class="col-md-6"><label class="form-label required" for="hGender">Giới tính</label>
                            <select class="form-select" id="hGender" name="gender" required><option value="M">Nam</option><option value="F">Nữ</option><option value="O">Khác</option></select></div>
                        <div class="col-md-6"><label class="form-label" for="hDob">Ngày sinh</label><input type="date" class="form-control" id="hDob" name="dateOfBirth"></div>
                        <div class="col-md-6"><label class="form-label required" for="hDate">Ngày vào làm</label>
                            <input type="date" class="form-control" id="hDate" name="hireDate" required><div class="invalid-feedback">Chọn ngày.</div></div>
                        <div class="col-md-6"><label class="form-label required" for="hSalary">Lương cơ bản</label>
                            <input type="number" class="form-control" id="hSalary" name="baseSalary" required min="0" step="500000" data-type="number">
                            <div class="invalid-feedback">Nhập lương.</div></div>
                        <div class="col-md-6"><label class="form-label" for="hAllowance">Phụ cấp</label>
                            <input type="number" class="form-control" id="hAllowance" name="allowance" min="0" step="100000" data-type="number"></div>
                        <div class="col-12"><label class="form-label" for="hManager">Quản lý trực tiếp</label>
                            <select class="form-select" id="hManager" name="managerId" data-type="number"></select>
                            <div class="form-text">Để trống = trưởng phòng của phòng ban tuyển dụng.</div></div>
                    </div>
                </div>
                <div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-success"><i class="bi bi-person-check me-1"></i>Tiếp nhận</button></div>
            </form>
        </div>
    </div>
    </c:if>
</t:layout>
