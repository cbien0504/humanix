$(function () {
    'use strict';
    var canEdit = $('#attendancePage').data('can-edit') === true;
    var today = new Date();
    var first = new Date(today.getFullYear(), today.getMonth(), 1);
    function iso(d) {
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    }
    $('#fromDate').val(iso(first));
    $('#toDate').val(iso(today));

    var columns = [
        {title: 'Ngày', sortKey: 'date', render: function (r) { return HRM.fmt.date(r.workDate); }},
        {title: 'Nhân viên', sortKey: 'employee', render: function (r) {
            return $('<div>').append($('<div class="fw-semibold">').text(r.fullName))
                .append($('<div class="small text-body-secondary">').text(r.employeeCode + ' · ' + r.departmentName));
        }},
        {title: 'Vào', render: function (r) { return HRM.fmt.time(r.checkIn); }},
        {title: 'Ra', render: function (r) { return HRM.fmt.time(r.checkOut); }},
        {title: 'Giờ làm', sortKey: 'hours', className: 'text-end', render: function (r) { return HRM.fmt.number(r.workHours, 2); }},
        {title: 'Trạng thái', sortKey: 'status', render: function (r) { return HRM.badge('attendanceStatus', r.status); }},
        {title: 'Muộn', sortKey: 'late', className: 'text-end', render: function (r) { return r.lateMinutes || ''; }},
        {title: 'Nguồn', key: 'source', className: 'small text-body-secondary'},
        {title: 'Ghi chú', key: 'note', className: 'small'}
    ];
    if (canEdit) {
        columns.push({title: '', className: 'table-actions', render: function () {
            return $('<div class="btn-group btn-group-sm">')
                .append($('<button class="btn btn-outline-primary act-edit" title="Điều chỉnh">').append('<i class="bi bi-pencil"></i>'))
                .append($('<button class="btn btn-outline-danger act-delete" title="Xóa">').append('<i class="bi bi-trash"></i>'));
        }});
    }
    var table = new HRM.DataTable($('#attendanceTable'), {
        url: '/api/attendance', filterForm: '#filterForm', size: 20, sort: 'date', dir: 'desc', columns: columns
    });
    table.reload();

    if (!canEdit) {
        return;
    }
    var $form = $('#attForm');
    var employeesLoaded = false;

    function open(values) {
        var ready = employeesLoaded ? $.Deferred().resolve() : HRM.get('/api/employees/options').done(function (list) {
            HRM.fillSelect($('#aEmployee'), list, 'employeeId', function (o) {
                return o.employeeCode + ' – ' + o.fullName;
            }, '— Chọn nhân viên —');
            employeesLoaded = true;
        });
        $.when(ready).done(function () {
            HRM.form.fill($form, values);
            HRM.modal('#attModal').show();
        });
    }

    $('#btnAdd').on('click', function () {
        open({workDate: iso(today), checkIn: '08:30', checkOut: '17:30'});
    });
    $('#attendanceTable').on('click', '.act-edit', function () {
        var r = $(this).closest('tr').data('row');
        open({employeeId: r.employeeId, workDate: r.workDate, checkIn: HRM.fmt.time(r.checkIn),
            checkOut: HRM.fmt.time(r.checkOut), status: r.status === 'ABSENT' ? 'ABSENT' : '', note: r.note});
    }).on('click', '.act-delete', function () {
        var r = $(this).closest('tr').data('row');
        HRM.confirm('Xóa bản ghi chấm công ngày ' + HRM.fmt.date(r.workDate) + ' của ' + r.fullName + '?',
            {variant: 'danger', okText: 'Xóa'}).done(function () {
            HRM.del('/api/attendance/' + r.attendanceId).done(function () {
                HRM.toast('Đã xóa bản ghi.');
                table.reload();
            });
        });
    });

    $form.on('submit', function (e) {
        e.preventDefault();
        var inT = $('#aIn').val();
        var outT = $('#aOut').val();
        $('#aOut')[0].setCustomValidity(inT && outT && outT <= inT ? 'Giờ ra phải sau giờ vào' : '');
        if (!HRM.form.validate($form) || !$form[0].checkValidity()) {
            return;
        }
        var data = HRM.form.toObject($form);
        HRM.post('/api/attendance', data, {form: $form}).done(function (r) {
            HRM.modal('#attModal').hide();
            HRM.toast('Đã lưu chấm công ' + r.employeeCode + ' ngày ' + HRM.fmt.date(r.workDate) + ' (' + HRM.label('attendanceStatus', r.status) + ')');
            table.reload();
        });
    });
});
