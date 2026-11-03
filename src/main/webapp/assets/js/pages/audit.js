$(function () {
    'use strict';
    var table = new HRM.DataTable($('#auditTable'), {
        url: '/api/admin/audit-logs',
        filterForm: '#filterForm',
        size: 20,
        columns: [
            {title: 'Thời gian', render: function (a) { return HRM.fmt.instant(a.createdAt); }},
            {title: 'Người dùng', key: 'username'},
            {title: 'Hành động', render: function (a) {
                var danger = /FAILED|LOCKED|DELETED|BLOCKED/.test(a.action);
                return $('<span class="badge">').addClass(danger ? 'text-bg-danger' : 'text-bg-light border').text(a.action);
            }},
            {title: 'Đối tượng', render: function (a) {
                return a.entityType ? a.entityType + (a.entityId ? ' #' + a.entityId : '') : '';
            }},
            {title: 'Chi tiết', key: 'details', className: 'small'},
            {title: 'IP', key: 'ipAddress', className: 'small text-body-secondary'}
        ]
    });
    table.reload();
});
