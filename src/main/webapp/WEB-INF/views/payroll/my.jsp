<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Phiếu lương của tôi" active="my-payslips" script="payroll-my">
    <div class="page-header"><div><h1 class="h4 mb-1">Phiếu lương của tôi</h1>
        <div class="subtitle">Chỉ hiển thị các kỳ lương đã được duyệt.</div></div></div>
    <div class="row g-3">
        <div class="col-lg-7">
            <div class="card shadow-sm">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead><tr><th>Kỳ</th><th class="text-end">Ngày công</th><th class="text-end">Gross</th><th class="text-end">Bảo hiểm</th>
                            <th class="text-end">Thuế</th><th class="text-end">Thực lĩnh</th><th></th></tr></thead>
                        <tbody id="payslips"></tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header"><i class="bi bi-calculator me-1"></i>Mô phỏng lương Gross → Net</div>
                <div class="card-body">
                    <form id="simForm" class="row g-2" novalidate>
                        <div class="col-12">
                            <label class="form-label required" for="sBase">Lương cơ bản (VND/tháng)</label>
                            <input type="number" class="form-control" id="sBase" name="baseSalary" required min="0" step="100000" data-type="number" value="20000000">
                        </div>
                        <div class="col-7">
                            <label class="form-label" for="sAllowance">Phụ cấp</label>
                            <input type="number" class="form-control" id="sAllowance" name="allowance" min="0" step="100000" data-type="number" value="0">
                        </div>
                        <div class="col-5">
                            <label class="form-label" for="sDep">Người phụ thuộc</label>
                            <input type="number" class="form-control" id="sDep" name="dependents" min="0" max="20" data-type="number" value="0">
                        </div>
                        <div class="col-12"><button class="btn btn-primary w-100" type="submit">Tính thử</button></div>
                    </form>
                    <table class="table table-sm mt-3 mb-0" id="simResult" hidden>
                        <tbody>
                        <tr><td>Tổng thu nhập</td><td class="money" data-k="grossSalary"></td></tr>
                        <tr><td>BHXH + BHYT + BHTN</td><td class="money" data-k="totalInsurance"></td></tr>
                        <tr><td>Thu nhập tính thuế</td><td class="money" data-k="taxableIncome"></td></tr>
                        <tr><td>Thuế TNCN</td><td class="money" data-k="personalIncomeTax"></td></tr>
                        <tr class="table-primary"><th>Thực lĩnh</th><th class="money" data-k="netSalary"></th></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</t:layout>
