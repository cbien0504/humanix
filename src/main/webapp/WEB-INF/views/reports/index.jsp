<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Báo cáo" active="reports" script="reports">
    <div class="page-header"><div><h1 class="h4 mb-1">Báo cáo nhân sự</h1>
        <div class="subtitle">Số liệu lấy từ stored procedure; xuất Excel (định dạng sẵn, có dòng tổng) hoặc CSV.</div></div></div>
    <ul class="nav nav-tabs mb-3" role="tablist">
        <li class="nav-item"><button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tabDept" type="button">Thống kê theo phòng ban</button></li>
        <li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabAtt" type="button">Báo cáo chấm công</button></li>
        <c:if test="${u.hr}"><li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabEmp" type="button">Danh sách nhân viên</button></li></c:if>
    </ul>
    <div class="tab-content">
        <div class="tab-pane fade show active" id="tabDept">
            <div class="card shadow-sm">
                <div class="card-body border-bottom d-flex flex-wrap gap-2 align-items-end">
                    <div><label class="form-label small mb-1" for="asOf">Tính đến ngày</label><input type="date" class="form-control" id="asOf"></div>
                    <div class="ms-auto d-flex gap-2">
                        <a class="btn btn-outline-success export" data-report="departments" data-format="xlsx" href="#"><i class="bi bi-file-earmark-excel me-1"></i>Excel</a>
                        <a class="btn btn-outline-secondary export" data-report="departments" data-format="csv" href="#"><i class="bi bi-filetype-csv me-1"></i>CSV</a>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead><tr><th>Phòng ban</th><th>Trưởng phòng</th><th class="text-end">Tổng NV</th><th class="text-end">Chính thức</th>
                            <th class="text-end">Thử việc</th><th class="text-end">Nam / Nữ</th><th class="text-end">Lương CB TB</th>
                            <th class="text-end">Quỹ lương/tháng</th><th class="text-end">Thâm niên TB</th><th class="text-end">Tuyển mới</th>
                            <th class="text-end">Nghỉ việc</th><th class="text-end">Tỷ lệ nghỉ việc</th></tr></thead>
                        <tbody id="deptRows"></tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="tab-pane fade" id="tabAtt">
            <div class="card shadow-sm">
                <div class="card-body border-bottom d-flex flex-wrap gap-2 align-items-end">
                    <div><label class="form-label small mb-1" for="attFrom">Từ ngày</label><input type="date" class="form-control" id="attFrom"></div>
                    <div><label class="form-label small mb-1" for="attTo">Đến ngày</label><input type="date" class="form-control" id="attTo"></div>
                    <div><label class="form-label small mb-1" for="attDept">Phòng ban</label>
                        <select class="form-select" id="attDept"><option value="">Tất cả (trong phạm vi)</option>
                            <c:forEach items="${departments}" var="d"><option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option></c:forEach>
                        </select></div>
                    <button class="btn btn-primary" id="btnAtt"><i class="bi bi-search me-1"></i>Xem</button>
                    <div class="ms-auto d-flex gap-2">
                        <a class="btn btn-outline-success export" data-report="attendance" data-format="xlsx" href="#"><i class="bi bi-file-earmark-excel me-1"></i>Excel</a>
                        <a class="btn btn-outline-secondary export" data-report="attendance" data-format="csv" href="#"><i class="bi bi-filetype-csv me-1"></i>CSV</a>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table table-sm table-hover align-middle mb-0">
                        <thead><tr><th>Nhân viên</th><th>Phòng ban</th><th class="text-end">Kỳ vọng</th><th class="text-end">Đúng giờ</th>
                            <th class="text-end">Muộn</th><th class="text-end">Nửa ngày</th><th class="text-end">Nghỉ có lương</th>
                            <th class="text-end">Nghỉ KL</th><th class="text-end">Vắng</th><th class="text-end">Ngày công</th>
                            <th class="text-end">Phút muộn</th><th class="text-end">Chuyên cần</th></tr></thead>
                        <tbody id="attRows"></tbody>
                    </table>
                </div>
            </div>
        </div>
        <c:if test="${u.hr}">
        <div class="tab-pane fade" id="tabEmp">
            <div class="card shadow-sm"><div class="card-body d-flex flex-wrap gap-2 align-items-end">
                <div><label class="form-label small mb-1" for="empDept">Phòng ban</label>
                    <select class="form-select" id="empDept"><option value="">Tất cả</option>
                        <c:forEach items="${departments}" var="d"><option value="${d.departmentId}"><c:out value="${d.departmentName}"/></option></c:forEach>
                    </select></div>
                <div><label class="form-label small mb-1" for="empStatus">Trạng thái</label>
                    <select class="form-select" id="empStatus"><option value="">Tất cả</option><option value="ACTIVE">Chính thức</option>
                        <option value="PROBATION">Thử việc</option><option value="ON_LEAVE">Nghỉ dài hạn</option><option value="TERMINATED">Đã nghỉ việc</option></select></div>
                <a class="btn btn-outline-success export" data-report="employees" data-format="xlsx" href="#"><i class="bi bi-file-earmark-excel me-1"></i>Xuất Excel</a>
                <a class="btn btn-outline-secondary export" data-report="employees" data-format="csv" href="#"><i class="bi bi-filetype-csv me-1"></i>Xuất CSV</a>
            </div></div>
        </div>
        </c:if>
    </div>
</t:layout>
