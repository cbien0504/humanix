<%@ tag description="Main layout: navbar, role-based sidebar, toasts, scripts" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="active" required="false" description="key of the highlighted menu entry" %>
<%@ attribute name="script" required="false" description="page script /assets/js/pages/{script}.js" %>
<%@ attribute name="charts" required="false" type="java.lang.Boolean" description="load Chart.js" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="u" value="${sessionScope.authUser}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="context-path" content="${ctx}">
    <meta name="csrf-token" content="<c:out value='${sessionScope.csrfToken}'/>">
    <title><c:out value="${title}"/> · <c:out value="${applicationScope.appName}"/></title>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${ctx}/webjars/bootstrap/5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/webjars/bootstrap-icons/1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=${applicationScope.assetVersion}">
</head>
<body>
<nav class="navbar navbar-dark app-navbar fixed-top">
    <div class="container-fluid">
        <button class="btn btn-link text-white d-lg-none me-1 p-1" type="button" data-bs-toggle="offcanvas"
                data-bs-target="#sidebar" aria-controls="sidebar" aria-label="Mở menu">
            <i class="bi bi-list fs-4"></i>
        </button>
        <a class="navbar-brand d-flex align-items-center gap-2" href="${ctx}/">
            <i class="bi bi-people-fill"></i><span>HRM</span>
        </a>
        <c:if test="${not empty u}">
            <div class="dropdown ms-auto">
                <button class="btn btn-link text-white text-decoration-none dropdown-toggle d-flex align-items-center gap-2"
                        data-bs-toggle="dropdown" aria-expanded="false">
                    <span class="avatar-circle"><c:out value="${fn:substring(u.fullName, 0, 1)}"/></span>
                    <span class="d-none d-sm-inline"><c:out value="${u.fullName}"/></span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                    <li><h6 class="dropdown-header"><c:out value="${u.username}"/>
                        <c:forEach items="${u.roles}" var="r"><span class="badge text-bg-light ms-1"><c:out value="${r.label}"/></span></c:forEach>
                    </h6></li>
                    <c:if test="${u.hasEmployee}">
                        <li><a class="dropdown-item" href="${ctx}/profile"><i class="bi bi-person-badge me-2"></i>Hồ sơ của tôi</a></li>
                    </c:if>
                    <li><a class="dropdown-item" href="${ctx}/profile/password"><i class="bi bi-key me-2"></i>Đổi mật khẩu</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li>
                        <form method="post" action="${ctx}/logout" class="m-0">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                            <button class="dropdown-item text-danger" type="submit"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</button>
                        </form>
                    </li>
                </ul>
            </div>
        </c:if>
    </div>
</nav>

<div class="app-shell">
    <aside class="app-sidebar offcanvas-lg offcanvas-start" tabindex="-1" id="sidebar" aria-label="Menu chính">
        <div class="offcanvas-header d-lg-none">
            <h5 class="offcanvas-title">Menu</h5>
            <button type="button" class="btn-close" data-bs-dismiss="offcanvas" data-bs-target="#sidebar" aria-label="Đóng"></button>
        </div>
        <nav class="nav flex-column app-menu">
            <a class="nav-link ${active == 'dashboard' ? 'active' : ''}" href="${ctx}/"><i class="bi bi-speedometer2"></i>Tổng quan</a>
            <c:if test="${not empty u and u.hasEmployee}">
                <div class="app-menu-section">Cá nhân</div>
                <a class="nav-link ${active == 'my-attendance' ? 'active' : ''}" href="${ctx}/attendance"><i class="bi bi-clock-history"></i>Chấm công</a>
                <a class="nav-link ${active == 'my-leave' ? 'active' : ''}" href="${ctx}/leave"><i class="bi bi-calendar2-check"></i>Nghỉ phép</a>
                <a class="nav-link ${active == 'my-payslips' ? 'active' : ''}" href="${ctx}/payroll/my"><i class="bi bi-receipt"></i>Phiếu lương</a>
                <a class="nav-link ${active == 'my-reviews' ? 'active' : ''}" href="${ctx}/performance"><i class="bi bi-star"></i>Đánh giá của tôi</a>
            </c:if>
            <c:if test="${not empty u and (u.hr or u.manager)}">
                <div class="app-menu-section">Quản lý</div>
                <a class="nav-link ${active == 'employees' ? 'active' : ''}" href="${ctx}/employees"><i class="bi bi-people"></i>Nhân viên</a>
                <c:if test="${u.hr}">
                    <a class="nav-link ${active == 'departments' ? 'active' : ''}" href="${ctx}/departments"><i class="bi bi-diagram-3"></i>Phòng ban</a>
                    <a class="nav-link ${active == 'positions' ? 'active' : ''}" href="${ctx}/positions"><i class="bi bi-briefcase"></i>Chức danh</a>
                </c:if>
                <a class="nav-link ${active == 'attendance' ? 'active' : ''}" href="${ctx}/attendance/manage"><i class="bi bi-calendar-week"></i>Dữ liệu chấm công</a>
                <a class="nav-link ${active == 'leave-approvals' ? 'active' : ''}" href="${ctx}/leave/approvals"><i class="bi bi-check2-square"></i>Duyệt nghỉ phép</a>
                <c:if test="${u.hr}">
                    <a class="nav-link ${active == 'payroll' ? 'active' : ''}" href="${ctx}/payroll"><i class="bi bi-cash-stack"></i>Bảng lương</a>
                </c:if>
                <a class="nav-link ${active == 'recruitment' ? 'active' : ''}" href="${ctx}/recruitment"><i class="bi bi-person-plus"></i>Tuyển dụng</a>
                <a class="nav-link ${active == 'performance' ? 'active' : ''}" href="${ctx}/performance/manage"><i class="bi bi-graph-up-arrow"></i>Đánh giá hiệu suất</a>
                <a class="nav-link ${active == 'reports' ? 'active' : ''}" href="${ctx}/reports"><i class="bi bi-bar-chart"></i>Báo cáo</a>
                <c:if test="${u.hr}">
                    <a class="nav-link ${active == 'import' ? 'active' : ''}" href="${ctx}/import"><i class="bi bi-upload"></i>Import dữ liệu</a>
                </c:if>
            </c:if>
            <c:if test="${not empty u and u.admin}">
                <div class="app-menu-section">Hệ thống</div>
                <a class="nav-link ${active == 'users' ? 'active' : ''}" href="${ctx}/admin/users"><i class="bi bi-shield-lock"></i>Tài khoản</a>
                <a class="nav-link ${active == 'audit' ? 'active' : ''}" href="${ctx}/admin/audit"><i class="bi bi-journal-text"></i>Nhật ký hệ thống</a>
                <a class="nav-link ${active == 'data-quality' ? 'active' : ''}" href="${ctx}/admin/data-quality"><i class="bi bi-clipboard-check"></i>Kiểm tra dữ liệu</a>
                <a class="nav-link ${active == 'settings' ? 'active' : ''}" href="${ctx}/admin/settings"><i class="bi bi-sliders"></i>Tham số lương</a>
            </c:if>
        </nav>
        <div class="app-sidebar-footer small text-body-secondary">
            v<c:out value="${applicationScope.appVersion}"/>
        </div>
    </aside>

    <main class="app-main">
        <jsp:doBody/>
    </main>
</div>

<div class="toast-container position-fixed bottom-0 end-0 p-3" id="toastArea" aria-live="polite"></div>

<script src="${ctx}/webjars/jquery/3.7.1/dist/jquery.min.js"></script>
<script src="${ctx}/webjars/bootstrap/5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="${ctx}/assets/js/app.js?v=${applicationScope.assetVersion}"></script>
<c:if test="${charts}">
    <script src="${ctx}/webjars/chart.js/4.4.1/dist/chart.umd.js"></script>
    <script src="${ctx}/assets/js/charts.js?v=${applicationScope.assetVersion}"></script>
</c:if>
<c:if test="${not empty script}">
    <script src="${ctx}/assets/js/pages/${script}.js?v=${applicationScope.assetVersion}"></script>
</c:if>
</body>
</html>
