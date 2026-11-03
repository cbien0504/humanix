<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Tổng quan" active="dashboard" script="dashboard" charts="${u.hr}">
    <div class="page-header">
        <div>
            <h1 class="h4 mb-1">Xin chào, <c:out value="${u.fullName}"/></h1>
            <div class="subtitle" id="todayLabel">Tổng quan hoạt động nhân sự</div>
        </div>
    </div>

    <section id="meSection" class="mb-4" hidden>
        <div class="row g-3">
            <div class="col-sm-6 col-xl-3">
                <a class="card stat-card h-100 text-decoration-none" href="${ctx}/attendance">
                    <div class="card-body d-flex gap-3 align-items-center">
                        <div class="stat-icon bg-soft-primary"><i class="bi bi-clock-history"></i></div>
                        <div><div class="stat-label">Chấm công hôm nay</div><div class="fw-semibold text-body" id="meAttendance">—</div></div>
                    </div>
                </a>
            </div>
            <div class="col-sm-6 col-xl-3">
                <a class="card stat-card h-100 text-decoration-none" href="${ctx}/leave">
                    <div class="card-body d-flex gap-3 align-items-center">
                        <div class="stat-icon bg-soft-success"><i class="bi bi-calendar2-heart"></i></div>
                        <div><div class="stat-label">Phép năm còn lại</div><div class="stat-value text-body" id="meLeave">—</div></div>
                    </div>
                </a>
            </div>
            <div class="col-sm-6 col-xl-3">
                <a class="card stat-card h-100 text-decoration-none" href="${ctx}/payroll/my" id="mePayslipLink">
                    <div class="card-body d-flex gap-3 align-items-center">
                        <div class="stat-icon bg-soft-info"><i class="bi bi-receipt"></i></div>
                        <div><div class="stat-label" id="mePayslipLabel">Phiếu lương gần nhất</div><div class="stat-value text-body" id="mePayslip">—</div></div>
                    </div>
                </a>
            </div>
            <div class="col-sm-6 col-xl-3">
                <div class="card stat-card h-100">
                    <div class="card-body">
                        <div class="stat-label mb-1">Việc cần làm</div>
                        <ul class="list-unstyled small mb-0" id="meTodo"></ul>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section id="teamSection" class="mb-4" hidden>
        <h2 class="h6 text-uppercase text-body-secondary mb-2">Đội của tôi</h2>
        <div class="row g-3">
            <div class="col-sm-4"><div class="card stat-card h-100"><div class="card-body">
                <div class="stat-label">Thành viên đã chấm công hôm nay</div><div class="stat-value" id="teamCheckin">—</div></div></div></div>
            <div class="col-sm-4"><a class="card stat-card h-100 text-decoration-none" href="${ctx}/leave/approvals"><div class="card-body">
                <div class="stat-label">Đơn nghỉ chờ tôi duyệt</div><div class="stat-value text-body" id="teamApprovals">—</div></div></a></div>
            <div class="col-sm-4"><a class="card stat-card h-100 text-decoration-none" href="${ctx}/performance/manage"><div class="card-body">
                <div class="stat-label">Phiếu đánh giá cần chấm</div><div class="stat-value text-body" id="teamReviews">—</div></div></a></div>
        </div>
    </section>

    <section id="companySection" hidden>
        <h2 class="h6 text-uppercase text-body-secondary mb-2">Toàn công ty</h2>
        <div class="row g-3 mb-3">
            <div class="col-sm-6 col-xl-3"><div class="card stat-card h-100"><div class="card-body">
                <div class="stat-label">Tổng nhân sự</div><div class="stat-value" id="kHeadcount">—</div>
                <div class="small text-body-secondary" id="kHeadcountSub"></div></div></div></div>
            <div class="col-sm-6 col-xl-3"><div class="card stat-card h-100"><div class="card-body">
                <div class="stat-label">Đã chấm công hôm nay</div><div class="stat-value" id="kCheckin">—</div>
                <div class="small text-body-secondary" id="kCheckinSub"></div></div></div></div>
            <div class="col-sm-6 col-xl-3"><a class="card stat-card h-100 text-decoration-none" href="${ctx}/leave/approvals"><div class="card-body">
                <div class="stat-label">Đơn nghỉ chờ duyệt</div><div class="stat-value text-body" id="kPending">—</div>
                <div class="small text-body-secondary" id="kOnLeave"></div></div></a></div>
            <div class="col-sm-6 col-xl-3"><a class="card stat-card h-100 text-decoration-none" href="${ctx}/recruitment"><div class="card-body">
                <div class="stat-label">Tuyển dụng</div><div class="stat-value text-body" id="kJobs">—</div>
                <div class="small text-body-secondary" id="kJobsSub"></div></div></a></div>
        </div>
        <div class="row g-3">
            <div class="col-xl-7">
                <div class="card shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-1">Chấm công 14 ngày làm việc gần nhất</h3>
                    <div class="small text-body-secondary mb-2">Số nhân viên theo trạng thái mỗi ngày</div>
                    <div class="chart-box"><canvas id="attendanceChart" role="img" aria-label="Biểu đồ cột chồng số nhân viên đúng giờ, đi muộn, nửa ngày theo ngày"></canvas></div>
                    <div id="attendanceTable"></div>
                </div></div>
            </div>
            <div class="col-xl-5">
                <div class="card shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-1">Nhân sự theo phòng ban</h3>
                    <div class="small text-body-secondary mb-2">Số nhân viên đang làm việc</div>
                    <div class="chart-box"><canvas id="departmentChart" role="img" aria-label="Biểu đồ thanh số nhân viên theo phòng ban"></canvas></div>
                    <div id="departmentTable"></div>
                </div></div>
            </div>
            <div class="col-xl-5">
                <div class="card shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-3">Cơ cấu nhân sự</h3>
                    <div class="small text-body-secondary mb-1">Giới tính</div>
                    <div id="genderBar" class="mb-3"></div>
                    <div class="small text-body-secondary mb-1">Loại hợp đồng / trạng thái</div>
                    <div id="statusBar"></div>
                </div></div>
            </div>
            <div class="col-xl-7">
                <div class="card shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-1">Tổng lương thực lĩnh theo kỳ</h3>
                    <div class="small text-body-secondary mb-2">Các kỳ lương đã tính gần nhất (VND)</div>
                    <div class="chart-box"><canvas id="payrollChart" role="img" aria-label="Biểu đồ cột tổng lương thực lĩnh theo kỳ"></canvas></div>
                    <div id="payrollTable"></div>
                </div></div>
            </div>
        </div>
    </section>
</t:layout>
