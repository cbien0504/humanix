<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Phòng ban" active="departments" script="departments">
    <div class="page-header">
        <div><h1 class="h4 mb-1">Phòng ban</h1><div class="subtitle">Cơ cấu tổ chức và trưởng phòng.</div></div>
        <button class="btn btn-primary" id="btnAdd"><i class="bi bi-plus-lg me-1"></i>Thêm phòng ban</button>
    </div>
    <div class="card shadow-sm">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead><tr><th>Mã</th><th>Tên phòng ban</th><th>Trưởng phòng</th><th class="text-end">Số NV</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody id="rows"></tbody>
            </table>
        </div>
    </div>
    <div class="modal fade" id="deptModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="deptForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Phòng ban</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <input type="hidden" name="departmentId">
                    <div class="row g-3">
                        <div class="col-4">
                            <label class="form-label required" for="dCode">Mã</label>
                            <input class="form-control" id="dCode" name="departmentCode" required maxlength="20" pattern="[A-Z0-9_\-]{2,20}" data-uppercase>
                            <div class="invalid-feedback">2–20 ký tự in hoa/số.</div>
                        </div>
                        <div class="col-8">
                            <label class="form-label required" for="dName">Tên phòng ban</label>
                            <input class="form-control" id="dName" name="departmentName" required maxlength="150">
                            <div class="invalid-feedback">Vui lòng nhập tên.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="dManager">Trưởng phòng</label>
                            <select class="form-select" id="dManager" name="managerId" data-type="number"></select>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="dDesc">Mô tả</label>
                            <textarea class="form-control" id="dDesc" name="description" rows="2" maxlength="500"></textarea>
                        </div>
                        <div class="col-12 form-check form-switch ms-2">
                            <input class="form-check-input" type="checkbox" role="switch" id="dActive" name="active">
                            <label class="form-check-label" for="dActive">Đang hoạt động</label>
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
</t:layout>
