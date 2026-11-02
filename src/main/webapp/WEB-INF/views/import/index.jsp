<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Import dữ liệu" active="import" script="import">
    <div class="page-header"><div><h1 class="h4 mb-1">Import nhân viên từ CSV / Excel</h1>
        <div class="subtitle">Chuyển dữ liệu từ hệ thống cũ (data migration). Mỗi dòng được kiểm tra đầy đủ như khi nhập tay; lỗi được báo theo dòng và cột.</div></div></div>
    <div class="row g-3">
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-body">
                    <ol class="small ps-3">
                        <li>Tải <a href="${pageContext.request.contextPath}/import/template.csv">file mẫu CSV</a> (UTF-8) hoặc dùng file .xlsx có cùng tiêu đề cột.</li>
                        <li>Cột bắt buộc: <code>employee_code, full_name, gender, email, department_code, position_code, hire_date, base_salary</code>.</li>
                        <li>Ngày dạng <code>yyyy-MM-dd</code> hoặc <code>dd/MM/yyyy</code>; quản lý (<code>manager_code</code>) phải có sẵn hoặc nằm ở dòng phía trên.</li>
                        <li>Nên chạy <strong>Kiểm tra</strong> trước, sửa lỗi rồi mới import.</li>
                    </ol>
                    <form id="importForm" novalidate>
                        <div class="mb-3">
                            <label class="form-label required" for="file">File dữ liệu</label>
                            <input type="file" class="form-control" id="file" name="file" accept=".csv,.xlsx" required>
                            <div class="invalid-feedback">Chọn file .csv hoặc .xlsx (tối đa 5 MB).</div>
                        </div>
                        <div class="mb-3">
                            <div class="form-check"><input class="form-check-input" type="radio" name="mode" id="m1" value="validate" checked>
                                <label class="form-check-label" for="m1"><strong>Chỉ kiểm tra</strong> – không ghi dữ liệu</label></div>
                            <div class="form-check"><input class="form-check-input" type="radio" name="mode" id="m2" value="valid-only">
                                <label class="form-check-label" for="m2"><strong>Import dòng hợp lệ</strong>, bỏ qua dòng lỗi</label></div>
                            <div class="form-check"><input class="form-check-input" type="radio" name="mode" id="m3" value="all-or-nothing">
                                <label class="form-check-label" for="m3"><strong>Tất cả hoặc không</strong> – chỉ import khi mọi dòng hợp lệ</label></div>
                        </div>
                        <button class="btn btn-primary" type="submit" id="btnUpload"><i class="bi bi-upload me-1"></i>Thực hiện</button>
                    </form>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="card shadow-sm" id="resultCard" hidden>
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span id="resultTitle">Kết quả</span>
                    <button class="btn btn-sm btn-outline-secondary" id="btnErrors" hidden><i class="bi bi-download me-1"></i>Báo cáo lỗi (CSV)</button>
                </div>
                <div class="card-body">
                    <div class="row text-center g-2 mb-3">
                        <div class="col-3"><div class="stat-value" id="rTotal">0</div><div class="stat-label">Dòng dữ liệu</div></div>
                        <div class="col-3"><div class="stat-value text-success" id="rValid">0</div><div class="stat-label">Hợp lệ</div></div>
                        <div class="col-3"><div class="stat-value text-danger" id="rInvalid">0</div><div class="stat-label">Dòng lỗi</div></div>
                        <div class="col-3"><div class="stat-value text-primary" id="rImported">0</div><div class="stat-label">Đã import</div></div>
                    </div>
                    <div class="table-responsive" style="max-height: 420px">
                        <table class="table table-sm align-middle mb-0">
                            <thead class="sticky-top"><tr><th>Dòng</th><th>Cột</th><th>Giá trị</th><th>Lỗi</th></tr></thead>
                            <tbody id="errorRows"></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</t:layout>
