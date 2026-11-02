/* Payroll periods: calculate, approve, mark paid, export (UC-30 … UC-33) */
$(function () {
    'use strict';
    var now = new Date();
    var $year = $('#yearFilter');
    for (var y = now.getFullYear(); y >= now.getFullYear() - 3; y--) {
        $year.append($('<option>').val(y).text('Năm ' + y));
    }
    var prev = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    $('#calcMonth').val(prev.getFullYear() + '-' + String(prev.getMonth() + 1).padStart(2, '0'));

    function load() {
        HRM.get('/api/payroll/periods', {year: $year.val()}).done(function (list) {
            var $rows = $('#periods').empty();
            if (!list.length) {
                $rows.append($('<tr>').append($('<td colspan="8" class="empty-state">').text('Chưa có kỳ lương nào trong năm.')));
            }
            $.each(list, function (_, p) {
                var label = String(p.periodMonth).padStart(2, '0') + '/' + p.periodYear;
                var $actions = $('<div class="btn-group btn-group-sm">')
                    .append($('<a class="btn btn-outline-secondary" title="Xem bảng lương">').attr('href', HRM.url('/payroll/' + p.periodId))
                        .append('<i class="bi bi-table"></i>'));
                if (p.status === 'OPEN' || p.status === 'CALCULATED') {
                    $actions.append($('<button class="btn btn-outline-primary act-recalc" title="Tính lại">').append('<i class="bi bi-arrow-repeat"></i>'));
                }
                if (p.status === 'CALCULATED') {
                    $actions.append($('<button class="btn btn-primary act-approve">').text('Duyệt'));
                }
                if (p.status === 'APPROVED') {
                    $actions.append($('<button class="btn btn-success act-pay">').text('Đã chi trả'));
                }
                $actions.append($('<a class="btn btn-outline-success" title="Xuất Excel">')
                    .attr('href', HRM.url('/payroll/' + p.periodId + '/export?format=xlsx')).append('<i class="bi bi-file-earmark-excel"></i>'));
                $rows.append($('<tr>').data('row', p).append(
                    $('<td class="fw-semibold">').append($('<a>').attr('href', HRM.url('/payroll/' + p.periodId)).text(label)),
                    $('<td class="text-end">').text(HRM.fmt.number(p.standardWorkDays)),
                    $('<td class="text-end">').text(p.employeeCount === null ? '' : p.employeeCount),
                    $('<td class="money">').text(HRM.fmt.money(p.totalGross)),
                    $('<td class="money fw-semibold">').text(HRM.fmt.money(p.totalNet)),
                    $('<td>').append(HRM.badge('payrollStatus', p.status)),
                    $('<td class="small text-body-secondary">').append(
                        $('<div>').text(p.calculatedAt ? 'Tính: ' + HRM.fmt.instant(p.calculatedAt) + ' · ' + (p.calculatedBy || '') : ''),
                        $('<div>').text(p.approvedAt ? 'Duyệt: ' + HRM.fmt.instant(p.approvedAt) + ' · ' + (p.approvedBy || '') : '')),
                    $('<td class="table-actions">').append($actions)));
            });
        });
    }

    function calculate(year, month) {
        HRM.toast('Đang tính lương tháng ' + month + '/' + year + '…', 'info');
        HRM.post('/api/payroll/periods/calculate', {year: year, month: month}).done(function (p) {
            HRM.toast('Đã tính lương ' + p.employeeCount + ' nhân viên – tổng thực lĩnh ' + HRM.fmt.money(p.totalNet) + ' ₫');
            $year.val(p.periodYear);
            load();
        });
    }

    $('#calcForm').on('submit', function (e) {
        e.preventDefault();
        var v = $('#calcMonth').val();
        if (!v) {
            return;
        }
        var parts = v.split('-');
        calculate(Number(parts[0]), Number(parts[1]));
    });

    $('#periods').on('click', '.act-recalc', function () {
        var p = $(this).closest('tr').data('row');
        HRM.confirm('Tính lại lương tháng ' + p.periodMonth + '/' + p.periodYear + ' theo dữ liệu chấm công mới nhất?', {okText: 'Tính lại'})
            .done(function () { calculate(p.periodYear, p.periodMonth); });
    }).on('click', '.act-approve', function () {
        var p = $(this).closest('tr').data('row');
        HRM.confirm('Duyệt bảng lương tháng ' + p.periodMonth + '/' + p.periodYear + '? Sau khi duyệt số liệu bị khóa, nhân viên xem được phiếu lương.',
            {okText: 'Duyệt'}).done(function () {
            HRM.post('/api/payroll/periods/' + p.periodId + '/approve').done(function () {
                HRM.toast('Đã duyệt bảng lương.');
                load();
            });
        });
    }).on('click', '.act-pay', function () {
        var p = $(this).closest('tr').data('row');
        HRM.confirm('Xác nhận đã chi trả lương tháng ' + p.periodMonth + '/' + p.periodYear + '?', {okText: 'Xác nhận', variant: 'success'})
            .done(function () {
                HRM.post('/api/payroll/periods/' + p.periodId + '/pay').done(function () {
                    HRM.toast('Đã đánh dấu chi trả.');
                    load();
                });
            });
    });

    $year.on('change', load);
    load();
});
