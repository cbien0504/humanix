/* My performance reviews (UC-51) */
$(function () {
    'use strict';
    var table = new HRM.DataTable($('#reviewTable'), {
        url: '/api/reviews/my',
        columns: [
            {title: 'Kỳ đánh giá', key: 'cycleName', className: 'fw-semibold'},
            {title: 'Giai đoạn', render: function (r) { return HRM.fmt.date(r.periodStart) + ' – ' + HRM.fmt.date(r.periodEnd); }},
            {title: 'Người đánh giá', key: 'reviewerName'},
            {title: 'Trạng thái', render: function (r) { return HRM.badge('reviewStatus', r.status); }},
            {title: 'Tự ĐG', className: 'text-end', render: function (r) { return HRM.fmt.number(r.selfScore, 2); }},
            {title: 'Quản lý', className: 'text-end', render: function (r) { return HRM.fmt.number(r.managerScore, 2); }},
            {title: 'Điểm cuối', className: 'text-end fw-semibold', render: function (r) { return HRM.fmt.number(r.finalScore, 2); }},
            {title: 'Xếp loại', className: 'text-center', render: function (r) {
                return r.rating ? $('<span class="badge text-bg-primary rating-badge">').text(r.rating) : '';
            }},
            {title: '', className: 'table-actions', render: function (r) {
                var self = r.status === 'PENDING_SELF' && r.cycleStatus === 'OPEN';
                return $('<a class="btn btn-sm">').addClass(self ? 'btn-primary' : 'btn-outline-secondary')
                    .attr('href', HRM.url('/performance/reviews/' + r.reviewId)).text(self ? 'Tự đánh giá' : 'Xem');
            }}
        ]
    });
    table.reload();
});
