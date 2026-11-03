$(function () {
    'use strict';
    function iso(d) {
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    }
    var today = new Date();
    $('#asOf').val(iso(today));
    $('#attFrom').val(iso(new Date(today.getFullYear(), today.getMonth() - 1, 1)));
    $('#attTo').val(iso(new Date(today.getFullYear(), today.getMonth(), 0)));

    function cell(text, cls) {
        return $('<td>').addClass(cls || '').text(text === null || text === undefined ? '' : text);
    }

    function loadDepartments() {
        HRM.get('/api/reports/departments', {asOf: $('#asOf').val()}).done(function (rows) {
            var $b = $('#deptRows').empty();
            $.each(rows, function (_, s) {
                $b.append($('<tr>').append(cell(s.departmentName, 'fw-semibold'), cell(s.managerName),
                    cell(s.headcount, 'text-end'), cell(s.activeCount, 'text-end'), cell(s.probationCount, 'text-end'),
                    cell(s.maleCount + ' / ' + s.femaleCount, 'text-end'), cell(HRM.fmt.money(s.avgBaseSalary), 'money'),
                    cell(HRM.fmt.money(s.monthlySalaryFund), 'money'), cell(HRM.fmt.number(s.avgYearsOfService) + ' năm', 'text-end'),
                    cell(s.newHiresYtd, 'text-end'), cell(s.terminationsYtd, 'text-end'), cell(HRM.fmt.number(s.turnoverRate) + '%', 'text-end')));
            });
        });
    }

    function loadAttendance() {
        HRM.get('/api/reports/attendance', {from: $('#attFrom').val(), to: $('#attTo').val(), departmentId: $('#attDept').val() || undefined})
            .done(function (rows) {
                var $b = $('#attRows').empty();
                if (!rows.length) {
                    $b.append($('<tr>').append($('<td colspan="12" class="empty-state">').text('Không có dữ liệu.')));
                }
                $.each(rows, function (_, r) {
                    $b.append($('<tr>').append(
                        $('<td>').append($('<div class="fw-semibold">').text(r.fullName), $('<div class="small text-body-secondary">').text(r.employeeCode)),
                        cell(r.departmentName, 'small'), cell(r.expectedDays, 'text-end'), cell(r.presentDays, 'text-end'),
                        cell(r.lateDays, 'text-end'), cell(r.halfDays, 'text-end'), cell(r.paidLeaveDays, 'text-end'),
                        cell(r.unpaidLeaveDays, 'text-end'), cell(r.absentDays, 'text-end'), cell(HRM.fmt.number(r.workedDays), 'text-end'),
                        cell(r.lateMinutes, 'text-end'), cell(HRM.fmt.number(r.attendanceRate) + '%', 'text-end fw-semibold')));
                });
            });
    }

    $(document).on('click', 'a.export', function (e) {
        e.preventDefault();
        var report = $(this).data('report');
        var params = {format: $(this).data('format')};
        if (report === 'departments') {
            params.asOf = $('#asOf').val();
        } else if (report === 'attendance') {
            params.from = $('#attFrom').val();
            params.to = $('#attTo').val();
            if ($('#attDept').val()) {
                params.departmentId = $('#attDept').val();
            }
        } else {
            if ($('#empDept').val()) {
                params.departmentId = $('#empDept').val();
            }
            if ($('#empStatus').val()) {
                params.status = $('#empStatus').val();
            }
        }
        window.location.href = HRM.url('/reports/' + report + '/export?' + $.param(params));
    });

    $('#asOf').on('change', loadDepartments);
    $('#btnAtt').on('click', loadAttendance);
    loadDepartments();
    loadAttendance();
});
