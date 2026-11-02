<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Chức danh" active="positions" script="positions">
    <div class="page-header">
        <div><h1 class="h4 mb-1">Chức danh &amp; khung lương</h1><div class="subtitle">Lương cơ bản của nhân viên phải nằm trong khung lương của chức danh (BR-03).</div></div>
        <button class="btn btn-primary" id="btnAdd"><i class="bi bi-plus-lg me-1"></i>Thêm chức danh</button>
    </div>
    <div class="card shadow-sm">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead><tr><th>Mã</th><th>Chức danh</th><th class="text-center">Cấp bậc</th><th class="text-end">Lương tối thiểu</th><th class="text-end">Lương tối đa</th><th class="text-end">Số NV</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody id="rows"></tbody>
            </table>
        </div>
    </div>
    <div class="modal fade" id="posModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" id="posForm" novalidate>
                <div class="modal-header"><h5 class="modal-title">Chức danh</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none form-alert"></div>
                    <input type="hidden" name="positionId">
                    <div class="row g-3">
                        <div class="col-4">
                            <label class="form-label required" for="pCode">Mã</label>
                            <input class="form-control" id="pCode" name="positionCode" required maxlength="20" pattern="[A-Z0-9_\-]{2,20}" data-uppercase>
                            <div class="invalid-feedback">2–20 ký tự in hoa/số.</div>
                        </div>
                        <div class="col-8">
                            <label class="form-label required" for="pName">Tên chức danh</label>
                            <input class="form-control" id="pName" name="positionName" required maxlength="150">
                            <div class="invalid-feedback">Vui lòng nhập tên.</div>
                        </div>
                        <div class="col-4">
                            <label class="form-label required" for="pLevel">Cấp bậc</label>
                            <input type="number" class="form-control" id="pLevel" name="jobLevel" required min="1" max="10" data-type="number">
                            <div class="invalid-feedback">Từ 1 đến 10.</div>
                        </div>
                        <div class="col-4">
                            <label class="form-label required" for="pMin">Lương tối thiểu</label>
                            <input type="number" class="form-control" id="pMin" name="minSalary" required min="0" step="1000" data-type="number">
                            <div class="invalid-feedback">Không được âm.</div>
                        </div>
                        <div class="col-4">
                            <label class="form-label required" for="pMax">Lương tối đa</label>
                            <input type="number" class="form-control" id="pMax" name="maxSalary" required min="0" step="1000" data-type="number">
                            <div class="invalid-feedback">Phải ≥ lương tối thiểu.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="pDesc">Mô tả</label>
                            <textarea class="form-control" id="pDesc" name="description" rows="2" maxlength="500"></textarea>
                        </div>
                        <div class="col-12 form-check form-switch ms-2">
                            <input class="form-check-input" type="checkbox" role="switch" id="pActive" name="active">
                            <label class="form-check-label" for="pActive">Đang sử dụng</label>
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
