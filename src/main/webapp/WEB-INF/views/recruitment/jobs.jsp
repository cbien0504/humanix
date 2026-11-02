<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Tuyển dụng" active="recruitment" script="recruitment-jobs">
    <div class="page-header" id="jobsPage" data-can-edit="${u.hr}">
        <div><h1 class="h4 mb-1">Tin tuyển dụng</h1>
            <div class="subtitle"><c:out value="${u.hr ? 'Quản lý tin tuyển dụng và hồ sơ ứng viên.' : 'Tin tuyển dụng của phòng ban bạn quản lý.'}"/></div></div>
        <c:if test="${u.hr}"><button class="btn btn-primary" id="btnAdd"><i class="bi bi-plus-lg me-1"></i>Tạo tin tuyển dụng</button></c:if>
    </div>
    <div class="row g-3 mb-3" id="stats"></div>
    <div class="card shadow-sm">
        <div class="card-body border-bottom">
            <form class="filter-bar" id="filterForm">
                <input type="search" class="form-control" name="keyword" placeholder="Mã tin, tiêu đề…" aria-label="Tìm kiếm">
                <select class="form-select" name="status" aria-label="Trạng thái">
                    <option value="">Tất cả trạng thái</option><option value="DRAFT">Nháp</option><option value="OPEN">Đang tuyển</option>
                    <option value="ON_HOLD">Tạm dừng</option><option value="CLOSED">Đã đóng</option>
                </select>
                <select class="form-select" name="departmentId" aria-label="Phòng ban">
                    <option value="">Tất cả phòng ban</option>
                    <c:forEach items="${departments}" var="d"><option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option></c:forEach>
                </select>
            </form>
        </div>
        <div class="table-responsive"><table class="table table-hover align-middle mb-0" id="jobTable"><thead></thead><tbody></tbody></table></div>
        <div class="data-table-footer"></div>
    </div>

    <c:if test="${u.hr}">
    <div class="modal fade" id="jobModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
            <form class="modal-content" id="jobForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Tin tuyển dụng</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <input type="hidden" name="jobPostingId">
                    <div class="row g-3">
                        <div class="col-md-4"><label class="form-label required" for="jCode">Mã tin</label>
                            <input class="form-control" id="jCode" name="jobCode" required maxlength="20" data-rule="code" data-uppercase>
                            <div class="invalid-feedback">Chữ in hoa, số, gạch ngang (3–20).</div></div>
                        <div class="col-md-8"><label class="form-label required" for="jTitle">Tiêu đề</label>
                            <input class="form-control" id="jTitle" name="title" required maxlength="200">
                            <div class="invalid-feedback">Vui lòng nhập tiêu đề.</div></div>
                        <div class="col-md-4"><label class="form-label required" for="jDept">Phòng ban</label>
                            <select class="form-select" id="jDept" name="departmentId" required data-type="number"><option value="">— Chọn —</option>
                                <c:forEach items="${departments}" var="d"><option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option></c:forEach>
                            </select><div class="invalid-feedback">Chọn phòng ban.</div></div>
                        <div class="col-md-4"><label class="form-label required" for="jPos">Chức danh</label>
                            <select class="form-select" id="jPos" name="positionId" required data-type="number"><option value="">— Chọn —</option>
                                <c:forEach items="${positions}" var="p"><option value="${p.positionId}"><c:out value="${p.positionName}"/></option></c:forEach>
                            </select><div class="invalid-feedback">Chọn chức danh.</div></div>
                        <div class="col-md-2"><label class="form-label required" for="jOpen">Số lượng</label>
                            <input type="number" class="form-control" id="jOpen" name="openings" required min="1" max="500" data-type="number">
                            <div class="invalid-feedback">1–500.</div></div>
                        <div class="col-md-2"><label class="form-label" for="jStatus">Trạng thái</label>
                            <select class="form-select" id="jStatus" name="status"><option value="DRAFT">Nháp</option><option value="OPEN">Đang tuyển</option>
                                <option value="ON_HOLD">Tạm dừng</option><option value="CLOSED">Đã đóng</option></select></div>
                        <div class="col-md-4"><label class="form-label" for="jType">Hình thức</label>
                            <select class="form-select" id="jType" name="employmentType"><option value="FULL_TIME">Toàn thời gian</option>
                                <option value="PART_TIME">Bán thời gian</option><option value="CONTRACT">Hợp đồng</option><option value="INTERN">Thực tập</option></select></div>
                        <div class="col-md-4"><label class="form-label" for="jLoc">Địa điểm</label><input class="form-control" id="jLoc" name="location" maxlength="100"></div>
                        <div class="col-md-2"><label class="form-label" for="jMin">Lương từ</label>
                            <input type="number" class="form-control" id="jMin" name="salaryMin" min="0" step="500000" data-type="number"></div>
                        <div class="col-md-2"><label class="form-label" for="jMax">Lương đến</label>
                            <input type="number" class="form-control" id="jMax" name="salaryMax" min="0" step="500000" data-type="number">
                            <div class="invalid-feedback">≥ lương từ.</div></div>
                        <div class="col-md-3"><label class="form-label" for="jOpenDate">Ngày mở</label><input type="date" class="form-control" id="jOpenDate" name="openDate"></div>
                        <div class="col-md-3"><label class="form-label" for="jCloseDate">Hạn nộp</label>
                            <input type="date" class="form-control" id="jCloseDate" name="closeDate" data-after="openDate">
                            <div class="invalid-feedback">Phải sau ngày mở.</div></div>
                        <div class="col-12"><label class="form-label" for="jDesc">Mô tả công việc</label><textarea class="form-control" id="jDesc" name="description" rows="3"></textarea></div>
                        <div class="col-12"><label class="form-label" for="jReq">Yêu cầu</label><textarea class="form-control" id="jReq" name="requirements" rows="3"></textarea></div>
                    </div>
                </div>
                <div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>Lưu</button></div>
            </form>
        </div>
    </div>
    </c:if>
</t:layout>
