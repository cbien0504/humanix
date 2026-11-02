<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="u" value="${sessionScope.authUser}"/>
<t:layout title="Tổng quan" active="dashboard">
    <div class="page-header">
        <div>
            <h1 class="h4 mb-1">Xin chào, <c:out value="${u.fullName}"/> 👋</h1>
            <div class="subtitle">Chúc bạn một ngày làm việc hiệu quả.</div>
        </div>
    </div>
    <div class="row g-3">
        <c:if test="${u.admin}">
            <div class="col-sm-6 col-xl-3">
                <a class="card stat-card h-100 text-decoration-none" href="${ctx}/admin/users">
                    <div class="card-body d-flex gap-3 align-items-center">
                        <div class="stat-icon bg-soft-primary"><i class="bi bi-shield-lock"></i></div>
                        <div><div class="fw-semibold text-body">Tài khoản</div><div class="stat-label">Tạo, phân quyền, khóa/mở</div></div>
                    </div>
                </a>
            </div>
            <div class="col-sm-6 col-xl-3">
                <a class="card stat-card h-100 text-decoration-none" href="${ctx}/admin/data-quality">
                    <div class="card-body d-flex gap-3 align-items-center">
                        <div class="stat-icon bg-soft-success"><i class="bi bi-clipboard-check"></i></div>
                        <div><div class="fw-semibold text-body">Kiểm tra dữ liệu</div><div class="stat-label">16 quy tắc toàn vẹn</div></div>
                    </div>
                </a>
            </div>
            <div class="col-sm-6 col-xl-3">
                <a class="card stat-card h-100 text-decoration-none" href="${ctx}/admin/audit">
                    <div class="card-body d-flex gap-3 align-items-center">
                        <div class="stat-icon bg-soft-warning"><i class="bi bi-journal-text"></i></div>
                        <div><div class="fw-semibold text-body">Nhật ký hệ thống</div><div class="stat-label">Đăng nhập, thay đổi dữ liệu</div></div>
                    </div>
                </a>
            </div>
        </c:if>
        <div class="col-sm-6 col-xl-3">
            <a class="card stat-card h-100 text-decoration-none" href="${ctx}/profile/password">
                <div class="card-body d-flex gap-3 align-items-center">
                    <div class="stat-icon bg-soft-secondary"><i class="bi bi-key"></i></div>
                    <div><div class="fw-semibold text-body">Đổi mật khẩu</div><div class="stat-label">Bảo vệ tài khoản của bạn</div></div>
                </div>
            </a>
        </div>
    </div>
</t:layout>
