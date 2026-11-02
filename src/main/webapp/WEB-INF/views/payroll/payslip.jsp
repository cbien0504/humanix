<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="http://hrm.example.com/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Phiếu lương ${p.periodLabel}" active="my-payslips" script="print">
    <div class="page-header no-print">
        <h1 class="h4 mb-0">Phiếu lương</h1>
        <button class="btn btn-outline-primary" id="btnPrint"><i class="bi bi-printer me-1"></i>In phiếu lương</button>
    </div>
    <div class="card shadow-sm mx-auto" style="max-width: 760px">
        <div class="card-body p-4">
            <div class="d-flex justify-content-between align-items-start mb-3">
                <div>
                    <div class="text-uppercase small text-body-secondary">Phiếu lương tháng</div>
                    <div class="h4 mb-0"><c:out value="${p.periodLabel}"/></div>
                </div>
                <div class="text-end small">
                    <div class="fw-semibold"><c:out value="${p.fullName}"/></div>
                    <div><c:out value="${p.employeeCode}"/> · <c:out value="${p.positionName}"/></div>
                    <div class="text-body-secondary"><c:out value="${p.departmentName}"/></div>
                </div>
            </div>
            <table class="table table-sm">
                <tbody>
                <tr class="table-light"><th colspan="2">Ngày công</th></tr>
                <tr><td>Ngày công chuẩn</td><td class="money"><c:out value="${h:number(p.standardDays)}"/></td></tr>
                <tr><td>Ngày đi làm</td><td class="money"><c:out value="${h:number(p.workedDays)}"/></td></tr>
                <tr><td>Nghỉ phép hưởng lương</td><td class="money"><c:out value="${h:number(p.paidLeaveDays)}"/></td></tr>
                <tr><td class="fw-semibold">Ngày công hưởng lương</td><td class="money fw-semibold"><c:out value="${h:number(p.paidDays)}"/></td></tr>
                <tr class="table-light"><th colspan="2">Thu nhập</th></tr>
                <tr><td>Lương cơ bản theo hợp đồng</td><td class="money"><c:out value="${h:money(p.baseSalary)}"/></td></tr>
                <tr><td>Lương theo ngày công</td><td class="money"><c:out value="${h:money(p.salaryAmount)}"/></td></tr>
                <tr><td>Phụ cấp</td><td class="money"><c:out value="${h:money(p.allowanceAmount)}"/></td></tr>
                <tr><td class="fw-semibold">Tổng thu nhập (Gross)</td><td class="money fw-semibold"><c:out value="${h:money(p.grossSalary)}"/></td></tr>
                <tr class="table-light"><th colspan="2">Khấu trừ</th></tr>
                <tr><td>BHXH (8%)</td><td class="money">−<c:out value="${h:money(p.socialInsurance)}"/></td></tr>
                <tr><td>BHYT (1,5%)</td><td class="money">−<c:out value="${h:money(p.healthInsurance)}"/></td></tr>
                <tr><td>BHTN (1%)</td><td class="money">−<c:out value="${h:money(p.unemploymentInsurance)}"/></td></tr>
                <tr><td class="text-body-secondary">Giảm trừ bản thân · <c:out value="${p.dependents}"/> người phụ thuộc</td>
                    <td class="money text-body-secondary"><c:out value="${h:money(p.personalDeduction)}"/> · <c:out value="${h:money(p.dependentDeduction)}"/></td></tr>
                <tr><td class="text-body-secondary">Thu nhập tính thuế</td><td class="money text-body-secondary"><c:out value="${h:money(p.taxableIncome)}"/></td></tr>
                <tr><td>Thuế TNCN</td><td class="money">−<c:out value="${h:money(p.personalIncomeTax)}"/></td></tr>
                <tr class="table-primary"><th>THỰC LĨNH (Net)</th><th class="money fs-5"><c:out value="${h:money(p.netSalary)}"/> ₫</th></tr>
                </tbody>
            </table>
            <div class="small text-body-secondary">
                Chuyển khoản: <c:out value="${p.bankName}"/> – <c:out value="${p.bankAccount}"/> ·
                Trạng thái kỳ lương: <c:out value="${p.periodStatus.label}"/>
            </div>
        </div>
    </div>
</t:layout>
