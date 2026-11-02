/* Review cycles (HR) and reviews to score (managers) – UC-50, UC-52, UC-53 */
$(function () {
    'use strict';
    var canEdit = $('#perfPage').data('can-edit') === true;

    var table = new HRM.DataTable($('#reviewTable'), {
        url: '/api/reviews', filterForm: '#filterForm', sort: 'employee',
        columns: [
            {title: 'Nhân viên', sortKey: 'employee', render: function (r) {
                return $('<div>').append($('<div class="fw-semibold">').text(r.fullName))
                    .append($('<div class="small text-body-secondary">').text(r.employeeCode + ' · ' + r.departmentName));
            }},
            {title: 'Kỳ', key: 'cycleName', sortKey: 'cycle', className: 'small'},
            {title: 'Người đánh giá', key: 'reviewerName', className: 'small'},
            {title: 'Trạng thái', sortKey: 'status', render: function (r) { return HRM.badge('reviewStatus', r.status); }},
            {title: 'Tự ĐG', className: 'text-end', render: function (r) { return HRM.fmt.number(r.selfScore, 2); }},
            {title: 'Quản lý', className: 'text-end', render: function (r) { return HRM.fmt.number(r.managerScore, 2); }},
            {title: 'Điểm cuối', sortKey: 'final', className: 'text-end fw-semibold', render: function (r) { return HRM.fmt.number(r.finalScore, 2); }},
            {title: 'Loại', className: 'text-center', render: function (r) {
                return r.rating ? $('<span class="badge text-bg-primary rating-badge">').text(r.rating) : '';
            }},
            {title: '', className: 'table-actions', render: function (r) {
                var score = !canEdit && r.status === 'PENDING_MANAGER' && r.cycleStatus === 'OPEN';
                return $('<a class="btn btn-sm">').addClass(score ? 'btn-primary' : 'btn-outline-secondary')
                    .attr('href', HRM.url('/performance/reviews/' + r.reviewId)).text(score ? 'Chấm điểm' : 'Xem');
            }}
        ]
    });
    table.reload();

    if (!canEdit) {
        return;
    }

    function loadCycles() {
        HRM.get('/api/review-cycles').done(function (list) {
            var $rows = $('#cycles').empty();
            $.each(list, function (_, c) {
                var pct = c.totalReviews ? Math.round(100 * c.completed / c.totalReviews) : 0;
                var $progress = $('<div>').append(
                    $('<div class="progress" style="height:8px">').append($('<div class="progress-bar bg-success">').css('width', pct + '%')),
                    $('<div class="small text-body-secondary mt-1">').text(c.completed + '/' + c.totalReviews + ' hoàn thành · '
                        + c.pendingSelf + ' chờ tự ĐG · ' + c.pendingManager + ' chờ QL'));
                var $act = $('<div class="btn-group btn-group-sm">');
                if (c.status === 'DRAFT') {
                    $act.append($('<button class="btn btn-success act-open">').text('Mở kỳ'))
                        .append($('<button class="btn btn-outline-danger act-delete">').append('<i class="bi bi-trash"></i>'));
                } else if (c.status === 'OPEN') {
                    $act.append($('<button class="btn btn-outline-dark act-close">').text('Đóng kỳ'));
                }
                $rows.append($('<tr>').data('row', c).append(
                    $('<td class="fw-semibold">').text(c.cycleName),
                    $('<td class="small">').text(HRM.fmt.date(c.periodStart) + ' – ' + HRM.fmt.date(c.periodEnd)),
                    $('<td class="small">').text(HRM.fmt.date(c.dueDate)),
                    $('<td>').append(HRM.badge('cycleStatus', c.status)),
                    $('<td>').append($progress),
                    $('<td class="text-end">').text(HRM.fmt.number(c.averageScore, 2)),
                    $('<td class="table-actions">').append($act)));
            });
        });
    }
    loadCycles();

    $('#cycles').on('click', '.act-open', function () {
        var c = $(this).closest('tr').data('row');
        HRM.confirm('Mở kỳ "' + c.cycleName + '"? Hệ thống sẽ tạo phiếu đánh giá cho mọi nhân viên đang làm việc có người quản lý.',
            {okText: 'Mở kỳ', variant: 'success'}).done(function () {
            HRM.post('/api/review-cycles/' + c.cycleId + '/open').done(function (cy) {
                HRM.toast('Đã mở kỳ – tạo ' + cy.totalReviews + ' phiếu đánh giá.');
                loadCycles();
                table.reload();
            });
        });
    }).on('click', '.act-close', function () {
        var c = $(this).closest('tr').data('row');
        HRM.confirm('Đóng kỳ "' + c.cycleName + '"? Phiếu chưa hoàn thành sẽ không thể chỉnh sửa.', {okText: 'Đóng kỳ', variant: 'dark'})
            .done(function () {
                HRM.post('/api/review-cycles/' + c.cycleId + '/close').done(function () {
                    HRM.toast('Đã đóng kỳ đánh giá.');
                    loadCycles();
                    table.reload();
                });
            });
    }).on('click', '.act-delete', function () {
        var c = $(this).closest('tr').data('row');
        HRM.confirm('Xóa kỳ nháp "' + c.cycleName + '"?', {variant: 'danger', okText: 'Xóa'}).done(function () {
            HRM.del('/api/review-cycles/' + c.cycleId).done(function () {
                HRM.toast('Đã xóa kỳ đánh giá.');
                loadCycles();
            });
        });
    });

    var $form = $('#cycleForm');
    $('#btnNewCycle').on('click', function () {
        HRM.form.fill($form, {selfWeight: 0.3});
        HRM.modal('#cycleModal').show();
    });
    $form.on('submit', function (e) {
        e.preventDefault();
        if (!HRM.form.validate($form)) {
            return;
        }
        HRM.post('/api/review-cycles', HRM.form.toObject($form), {form: $form}).done(function () {
            HRM.modal('#cycleModal').hide();
            HRM.toast('Đã tạo kỳ đánh giá (Nháp). Bấm "Mở kỳ" để bắt đầu.');
            loadCycles();
        });
    });
});
