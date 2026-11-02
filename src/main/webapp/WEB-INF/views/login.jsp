<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="context-path" content="${ctx}">
    <meta name="csrf-token" content="<c:out value='${sessionScope.csrfToken}'/>">
    <title>Đăng nhập · <c:out value="${applicationScope.appName}"/></title>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${ctx}/webjars/bootstrap/5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/webjars/bootstrap-icons/1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=${applicationScope.assetVersion}">
</head>
<body class="login-page d-flex align-items-center justify-content-center p-3">
<main class="w-100 d-flex flex-column align-items-center">
    <div class="card login-card shadow-sm border-0">
        <div class="card-body p-4 p-sm-5">
            <div class="d-flex align-items-center gap-3 mb-4">
                <div class="login-brand"><i class="bi bi-people-fill"></i></div>
                <div>
                    <h1 class="h5 mb-0">HRM System</h1>
                    <div class="text-body-secondary small">Hệ thống quản lý nhân sự</div>
                </div>
            </div>

            <c:if test="${param.logout == '1'}">
                <div class="alert alert-success py-2 small"><i class="bi bi-check-circle me-1"></i>Bạn đã đăng xuất.</div>
            </c:if>
            <c:if test="${param.expired == '1'}">
                <div class="alert alert-warning py-2 small"><i class="bi bi-clock-history me-1"></i>Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.</div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-danger py-2 small" role="alert"><i class="bi bi-exclamation-octagon me-1"></i><c:out value="${error}"/></div>
            </c:if>

            <form method="post" action="${ctx}/login" id="loginForm" novalidate>
                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                <input type="hidden" name="next" value="<c:out value='${next}'/>">
                <div class="mb-3">
                    <label for="username" class="form-label">Tên đăng nhập</label>
                    <div class="input-group has-validation">
                        <span class="input-group-text"><i class="bi bi-person"></i></span>
                        <input type="text" class="form-control" id="username" name="username" required maxlength="50"
                               autocomplete="username" autofocus value="<c:out value='${username}'/>">
                        <div class="invalid-feedback">Vui lòng nhập tên đăng nhập.</div>
                    </div>
                </div>
                <div class="mb-4">
                    <label for="password" class="form-label">Mật khẩu</label>
                    <div class="input-group has-validation">
                        <span class="input-group-text"><i class="bi bi-lock"></i></span>
                        <input type="password" class="form-control" id="password" name="password" required maxlength="100"
                               autocomplete="current-password">
                        <button class="btn btn-outline-secondary" type="button" id="togglePassword" aria-label="Hiện mật khẩu">
                            <i class="bi bi-eye"></i>
                        </button>
                        <div class="invalid-feedback">Vui lòng nhập mật khẩu.</div>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary w-100 py-2">
                    <i class="bi bi-box-arrow-in-right me-1"></i>Đăng nhập
                </button>
            </form>

            <c:if test="${showDemoAccounts}">
                <div class="mt-4 pt-3 border-top">
                    <div class="small text-body-secondary mb-2">Tài khoản demo (mật khẩu <code>Demo@123</code>):</div>
                    <div class="d-flex flex-wrap gap-2">
                        <button type="button" class="btn btn-sm btn-outline-secondary demo-login" data-user="admin">admin</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary demo-login" data-user="hr">hr</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary demo-login" data-user="manager">manager</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary demo-login" data-user="employee">employee</button>
                    </div>
                </div>
            </c:if>
        </div>
    </div>
    <p class="text-body-secondary small mt-3 mb-0">HRM System v<c:out value="${applicationScope.appVersion}"/></p>
</main>
<script src="${ctx}/webjars/jquery/3.7.1/dist/jquery.min.js"></script>
<script src="${ctx}/assets/js/pages/login.js?v=${applicationScope.assetVersion}"></script>
</body>
</html>
