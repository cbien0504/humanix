/* Payroll sheet of one period with department totals (UC-31) */
$(function () {
    'use strict';
    var id = $('#periodPage').data('period-id');

    HRM.get('/api/payroll/periods/' + id + '/departments').done(function (list) {
        var $t = $('#deptTotals').empty();
        $.each(list, function (_, d) {
            $t.append($('<tr>').append($('<td>').text(d.departmentName), $('<td class="text-end small">').text(d.employees + ' NV'),
                $('<td class="money small">').text(HRM.fmt.money(d.net))));
        });
    });

    var table = new HRM.DataTable($('#payslipTable'), {
        url: '/api/payroll/periods/' + id + '/payslips', filterForm: '#filterForm', size: 20, sort: 'code',
        columns: [
            {title: 'Mã NV', key: 'employeeCode', sortKey: 'code', className: 'fw-semibold'},
            {title: 'Họ tên', sortKey: 'name', render: function (p) {
                return $('<div>').append($('<div>').text(p.fullName)).append($('<div class="small text-body-secondary">').text(p.departmentName));
            }},
            {title: 'Công', className: 'text-end', render: function (p) {
                return HRM.fmt.number(p.paidDays) + '/' + HRM.fmt.number(p.standardDays);
            }},
            {title: 'Lương CB', className: 'money', render: function (p) { return HRM.fmt.money(p.baseSalary); }},
            {title: 'Gross', sortKey: 'gross', className: 'money', render: function (p) { return HRM.fmt.money(p.grossSalary); }},
            {title: 'Bảo hiểm', className: 'money', render: function (p) { return HRM.fmt.money(p.totalInsurance); }},
            {title: 'Thuế TNCN', sortKey: 'tax', className: 'money', render: function (p) { return HRM.fmt.money(p.personalIncomeTax); }},
            {title: 'Thực lĩnh', sortKey: 'net', className: 'money fw-semibold', render: function (p) { return HRM.fmt.money(p.netSalary); }},
            {title: '', className: 'table-actions', render: function (p) {
                return $('<a class="btn btn-sm btn-outline-secondary" title="Phiếu lương">')
                    .attr('href', HRM.url('/payroll/payslip/' + p.payrollId)).append('<i class="bi bi-receipt"></i>');
            }}
        ]
    });
    table.reload();
});
