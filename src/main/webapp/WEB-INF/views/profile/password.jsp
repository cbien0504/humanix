<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Đổi mật khẩu" script="password">
    <div class="page-header"><h1 class="h4 mb-0">Đổi mật khẩu</h1></div>
    <div class="row">
        <div class="col-lg-6 col-xl-5">
            <c:if test="${param.required == '1'}">
                <div class="alert alert-warning"><i class="bi bi-exclamation-triangle me-1"></i>
                    Bạn đang dùng mật khẩu tạm thời. Vui lòng đổi mật khẩu để tiếp tục sử dụng hệ thống.</div>
            </c:if>
            <div class="card shadow-sm">
                <div class="card-body">
                    <form id="passwordForm" novalidate>
                        <div class="alert alert-danger d-none form-alert"></div>
                        <div class="mb-3">
                            <label class="form-label required" for="currentPassword">Mật khẩu hiện tại</label>
                            <input type="password" class="form-control" id="currentPassword" name="currentPassword" required autocomplete="current-password">
                            <div class="invalid-feedback">Vui lòng nhập mật khẩu hiện tại.</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="newPassword">Mật khẩu mới</label>
                            <input type="password" class="form-control" id="newPassword" name="newPassword" required data-rule="password" autocomplete="new-password">
                            <div class="invalid-feedback">Tối thiểu 8 ký tự, có chữ hoa, chữ thường và chữ số.</div>
                            <div class="form-text">Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và chữ số.</div>
                        </div>
                        <div class="mb-4">
                            <label class="form-label required" for="confirmPassword">Nhập lại mật khẩu mới</label>
                            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required data-match="newPassword" autocomplete="new-password">
                            <div class="invalid-feedback">Mật khẩu nhập lại không khớp.</div>
                        </div>
                        <button class="btn btn-primary" type="submit"><i class="bi bi-check2 me-1"></i>Lưu mật khẩu</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</t:layout>
