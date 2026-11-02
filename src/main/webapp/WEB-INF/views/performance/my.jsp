<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Đánh giá của tôi" active="my-reviews" script="performance-my">
    <div class="page-header"><div><h1 class="h4 mb-1">Đánh giá hiệu suất của tôi</h1>
        <div class="subtitle">Tự đánh giá theo từng tiêu chí, sau đó quản lý trực tiếp chấm điểm. Điểm cuối = 30% tự đánh giá + 70% quản lý.</div></div></div>
    <div class="card shadow-sm">
        <div class="table-responsive"><table class="table table-hover align-middle mb-0" id="reviewTable"><thead></thead><tbody></tbody></table></div>
        <div class="data-table-footer"></div>
    </div>
</t:layout>
