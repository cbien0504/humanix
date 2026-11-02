/* Leave approval queue for managers and HR (UC-25, UC-27) */
$(function () {
    'use strict';
    var table = new HRM.DataTable($('#approvalTable'), {
        url: '/api/leave-requests/approvals', filterForm: '#filterForm', sort: 'startDate',
        columns: [
            {title: 'Nhân viên', sortKey: 'employee', render: function (r) {
                return $('<div>').append($('<div class="fw-semibold">').text(r.fullName))
                    .append($('<div class="small text-body-secondary">').text(r.employeeCode + ' · ' + r.departmentName));
            }},
            {title: 'Loại', render: function (r) {
                return $('<div>').append($('<div>').text(r.leaveTypeName))
                    .append($('<div class="small text-body-secondary">').text(r.paid ? 'Hưởng lương' : 'Không hưởng lương'));
            }},
            {title: 'Thời gian', sortKey: 'startDate', render: function (r) {
                return HRM.fmt.date(r.startDate) + (r.endDate !== r.startDate ? ' → ' + HRM.fmt.date(r.endDate) : '');
            }},
            {title: 'Ngày', sortKey: 'days', className: 'text-end', render: function (r) { return HRM.fmt.number(r.totalDays); }},
            {title: 'Lý do', key: 'reason', className: 'small'},
            {title: 'Gửi lúc', sortKey: 'createdAt', className: 'small', render: function (r) { return HRM.fmt.instant(r.createdAt); }},
            {title: 'Trạng thái', render: function (r) {
                var $s = $('<div>').append(HRM.badge('leaveStatus', r.status));
                if (r.approverName) {
                    $s.append($('<div class="small text-body-secondary">').text(r.approverName + (r.approverComment ? ': ' + r.approverComment : '')));
                }
                return $s;
            }},
            {title: '', className: 'table-actions', render: function (r) {
                if (r.status !== 'PENDING') {
                    return '';
                }
                return $('<div class="btn-group btn-group-sm">')
                    .append($('<button class="btn btn-success act-approve">').append('<i class="bi bi-check-lg me-1"></i>Duyệt'))
                    .append($('<button class="btn btn-outline-danger act-reject">').append('<i class="bi bi-x-lg me-1"></i>Từ chối'));
            }}
        ]
    });
    table.reload();

    $('#approvalTable').on('click', '.act-approve', function () {
        var r = $(this).closest('tr').data('row');
        HRM.confirm('Duyệt đơn ' + r.leaveTypeName + ' của ' + r.fullName + ' (' + HRM.fmt.number(r.totalDays) + ' ngày)?',
            {title: 'Duyệt đơn nghỉ', okText: 'Duyệt', variant: 'success', input: true, placeholder: 'Ghi chú (không bắt buộc)'})
            .done(function (comment) {
                HRM.post('/api/leave-requests/' + r.leaveRequestId + '/approve', {comment: comment}).done(function () {
                    HRM.toast('Đã duyệt đơn của ' + r.fullName);
                    table.reload();
                });
            });
    }).on('click', '.act-reject', function () {
        var r = $(this).closest('tr').data('row');
        HRM.confirm('Từ chối đơn ' + r.leaveTypeName + ' của ' + r.fullName + '?',
            {title: 'Từ chối đơn nghỉ', okText: 'Từ chối', variant: 'danger', input: true, inputRequired: true,
                placeholder: 'Lý do từ chối (bắt buộc)'})
            .done(function (comment) {
                HRM.post('/api/leave-requests/' + r.leaveRequestId + '/reject', {comment: comment}).done(function () {
                    HRM.toast('Đã từ chối đơn của ' + r.fullName, 'warning');
                    table.reload();
                });
            });
    });

    $('#btnInit').on('click', function () {
        var year = new Date().getFullYear();
        HRM.confirm('Khởi tạo / cập nhật số dư phép năm ' + year + ' cho toàn bộ nhân viên (12 ngày + thâm niên, tỷ lệ cho người mới)?',
            {okText: 'Khởi tạo'}).done(function () {
            HRM.post('/api/leave-requests/balances/init?year=' + year).done(function (res) {
                HRM.toast('Đã khởi tạo/cập nhật ' + res.rows + ' số dư phép năm ' + year + '.');
            });
        });
    });
});
