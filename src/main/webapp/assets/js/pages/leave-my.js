/* My leave: balances, requests, submit and cancel (UC-23, UC-24, UC-26) */
$(function () {
    'use strict';
    var $form = $('#leaveForm');
    var year = new Date().getFullYear();
    $('#yearFilter').val(year);

    function loadBalances() {
        HRM.get('/api/leave-requests/balances', {year: year}).done(function (list) {
            var $box = $('#balances').empty();
            if (!list.length) {
                $box.append($('<div class="col-12">').append($('<div class="alert alert-light border mb-0">')
                    .text('Chưa có số dư phép năm ' + year + '.')));
            }
            $.each(list, function (_, b) {
                var card = $('<div class="card stat-card h-100">').append($('<div class="card-body">').append(
                    $('<div class="d-flex justify-content-between align-items-start">').append(
                        $('<div>').append($('<div class="stat-label">').text(b.leaveTypeName + ' ' + b.year),
                            $('<div class="stat-value text-primary">').text(HRM.fmt.number(b.availableDays))
                                .append($('<span class="fs-6 text-body-secondary fw-normal">').text(' ngày khả dụng'))),
                        $('<div class="stat-icon bg-soft-primary">').append('<i class="bi bi-calendar2-heart"></i>')),
                    $('<div class="small text-body-secondary mt-2">').text('Được hưởng ' + HRM.fmt.number(b.entitledDays)
                        + ' · đã dùng ' + HRM.fmt.number(b.usedDays) + ' · đang chờ duyệt ' + HRM.fmt.number(b.pendingDays))));
                $box.append($('<div class="col-md-6 col-xl-4">').append(card));
            });
        });
    }

    var table = new HRM.DataTable($('#leaveTable'), {
        url: '/api/leave-requests/my', filterForm: '#filterForm', sort: 'startDate', dir: 'desc',
        columns: [
            {title: 'Loại nghỉ', key: 'leaveTypeName'},
            {title: 'Thời gian', sortKey: 'startDate', render: function (r) {
                return HRM.fmt.date(r.startDate) + (r.endDate !== r.startDate ? ' → ' + HRM.fmt.date(r.endDate) : '');
            }},
            {title: 'Số ngày', sortKey: 'days', className: 'text-end', render: function (r) { return HRM.fmt.number(r.totalDays); }},
            {title: 'Lý do', key: 'reason', className: 'small'},
            {title: 'Trạng thái', sortKey: 'status', render: function (r) { return HRM.badge('leaveStatus', r.status); }},
            {title: 'Người duyệt / ghi chú', render: function (r) {
                if (!r.approverName) {
                    return '';
                }
                return $('<div class="small">').append($('<div>').text(r.approverName + ' · ' + HRM.fmt.instant(r.decidedAt)))
                    .append($('<div class="text-body-secondary">').text(r.approverComment || ''));
            }},
            {title: '', className: 'table-actions', render: function (r) {
                var today = new Date().toISOString().substring(0, 10);
                if (r.status === 'PENDING' || (r.status === 'APPROVED' && r.startDate > today)) {
                    return $('<button class="btn btn-sm btn-outline-danger act-cancel">').append('<i class="bi bi-x-circle me-1"></i>Hủy');
                }
                return '';
            }}
        ]
    });
    table.reload();
    loadBalances();

    var preview = HRM.debounce(function () {
        var from = $('#lStart').val();
        var to = $('#lEnd').val();
        if (!from || !to || to < from) {
            $('#daysPreview').text('Chọn ngày để xem số ngày làm việc.');
            return;
        }
        HRM.get('/api/leave-requests/working-days', {from: from, to: to}, {silent: true}).done(function (res) {
            $('#daysPreview').text('Số ngày làm việc trong khoảng đã chọn: ' + res.days + ' ngày.');
        });
    }, 250);
    $('#lStart, #lEnd').on('change', function () {
        if ($(this).is('#lStart') && !$('#lEnd').val()) {
            $('#lEnd').val($('#lStart').val());
        }
        preview();
    });

    $('#btnNew').on('click', function () {
        HRM.form.fill($form, {});
        $('#daysPreview').text('Chọn ngày để xem số ngày làm việc.');
        HRM.modal('#leaveModal').show();
    });

    $form.on('submit', function (e) {
        e.preventDefault();
        if (!HRM.form.validate($form)) {
            return;
        }
        HRM.post('/api/leave-requests', HRM.form.toObject($form), {form: $form}).done(function (r) {
            HRM.modal('#leaveModal').hide();
            HRM.toast('Đã gửi đơn ' + r.leaveTypeName + ' (' + HRM.fmt.number(r.totalDays) + ' ngày), chờ quản lý duyệt.');
            table.reload(1);
            loadBalances();
        });
    });

    $('#leaveTable').on('click', '.act-cancel', function () {
        var r = $(this).closest('tr').data('row');
        HRM.confirm('Hủy đơn ' + r.leaveTypeName + ' từ ' + HRM.fmt.date(r.startDate) + '?', {variant: 'danger', okText: 'Hủy đơn'})
            .done(function () {
                HRM.post('/api/leave-requests/' + r.leaveRequestId + '/cancel').done(function () {
                    HRM.toast('Đã hủy đơn nghỉ phép.');
                    table.reload();
                    loadBalances();
                });
            });
    });
});
