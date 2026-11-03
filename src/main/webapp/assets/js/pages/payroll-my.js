$(function () {
    'use strict';
    HRM.get('/api/payroll/my').done(function (list) {
        var $rows = $('#payslips').empty();
        if (!list.length) {
            $rows.append($('<tr>').append($('<td colspan="7" class="empty-state">').text('Chưa có phiếu lương đã duyệt.')));
        }
        $.each(list, function (_, p) {
            $rows.append($('<tr>').append(
                $('<td class="fw-semibold">').text(String(p.periodMonth).padStart(2, '0') + '/' + p.periodYear),
                $('<td class="text-end">').text(HRM.fmt.number(p.paidDays) + '/' + HRM.fmt.number(p.standardDays)),
                $('<td class="money">').text(HRM.fmt.money(p.grossSalary)),
                $('<td class="money">').text(HRM.fmt.money(p.totalInsurance)),
                $('<td class="money">').text(HRM.fmt.money(p.personalIncomeTax)),
                $('<td class="money fw-semibold text-success">').text(HRM.fmt.money(p.netSalary)),
                $('<td class="table-actions">').append($('<a class="btn btn-sm btn-outline-primary">')
                    .attr('href', HRM.url('/payroll/payslip/' + p.payrollId)).text('Chi tiết'))));
        });
    });

    var $form = $('#simForm');
    $form.on('submit', function (e) {
        e.preventDefault();
        if (!HRM.form.validate($form)) {
            return;
        }
        HRM.post('/api/payroll/simulate', HRM.form.toObject($form), {form: $form}).done(function (r) {
            $('#simResult [data-k]').each(function () {
                $(this).text(HRM.fmt.money(r[$(this).data('k')]) + ' ₫');
            });
            $('#simResult').prop('hidden', false);
        });
    }).trigger('submit');
});
