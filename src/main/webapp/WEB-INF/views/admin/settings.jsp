<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Tham số lương" active="settings" script="settings">
    <div class="page-header"><div><h1 class="h4 mb-1">Tham số tính lương</h1>
        <div class="subtitle">Thay đổi có hiệu lực từ lần tính lương tiếp theo; kỳ đã duyệt không bị ảnh hưởng.</div></div></div>
    <div class="row g-3">
        <div class="col-lg-7">
            <div class="card shadow-sm">
                <form id="settingsForm" novalidate>
                    <div class="card-body" id="settingFields"></div>
                    <div class="card-footer bg-white text-end">
                        <button class="btn btn-primary" type="submit"><i class="bi bi-check2 me-1"></i>Lưu tham số</button>
                    </div>
                </form>
            </div>
        </div>
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header">Biểu thuế TNCN lũy tiến (tháng)</div>
                <table class="table table-sm mb-0">
                    <thead><tr><th>Bậc</th><th class="text-end">Từ</th><th class="text-end">Đến</th><th class="text-end">Thuế suất</th></tr></thead>
                    <tbody id="brackets"></tbody>
                </table>
            </div>
            <div class="alert alert-warning small mt-3 mb-0">Mô hình đơn giản hóa phục vụ demo – cần đối chiếu văn bản pháp luật hiện hành trước khi dùng thực tế.</div>
        </div>
    </div>
</t:layout>
