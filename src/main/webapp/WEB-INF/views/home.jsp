<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Trang chủ" active="dashboard" script="home">
    <div class="page-header">
        <h1 class="h4 mb-0">Hệ thống Quản lý Nhân sự</h1>
    </div>
    <div class="card shadow-sm">
        <div class="card-body">
            <p class="mb-3">Ứng dụng đã khởi động. Kiểm tra kết nối cơ sở dữ liệu:</p>
            <button class="btn btn-outline-primary" id="btnHealth"><i class="bi bi-heart-pulse me-1"></i>Kiểm tra /health</button>
            <pre class="mt-3 mb-0 bg-body-tertiary p-3 rounded small" id="healthResult" hidden></pre>
        </div>
    </div>
</t:layout>
