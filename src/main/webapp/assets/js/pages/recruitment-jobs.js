/* Job postings (UC-40) */
$(function () {
    'use strict';
    var canEdit = $('#jobsPage').data('can-edit') === true;
    var $form = $('#jobForm');
    var TYPES = {FULL_TIME: 'Toàn thời gian', PART_TIME: 'Bán thời gian', CONTRACT: 'Hợp đồng', INTERN: 'Thực tập'};

    function salary(j) {
        if (!j.salaryMin && !j.salaryMax) {
            return 'Thỏa thuận';
        }
        return HRM.fmt.money(j.salaryMin) + ' – ' + HRM.fmt.money(j.salaryMax);
    }

    var columns = [
        {title: 'Mã tin', key: 'jobCode', sortKey: 'code', className: 'fw-semibold'},
        {title: 'Vị trí', sortKey: 'title', render: function (j) {
            return $('<div>').append($('<a>').attr('href', HRM.url('/recruitment/jobs/' + j.jobPostingId)).text(j.title))
                .append($('<div class="small text-body-secondary">').text(j.departmentName + ' · ' + (TYPES[j.employmentType] || j.employmentType)));
        }},
        {title: 'Đã tuyển', className: 'text-center', render: function (j) {
            var pct = Math.min(100, Math.round(100 * j.hiredCount / Math.max(1, j.openings)));
            return $('<div style="min-width:90px">').append($('<div class="small">').text(j.hiredCount + '/' + j.openings),
                $('<div class="progress" style="height:6px">').append($('<div class="progress-bar bg-success">').css('width', pct + '%')));
        }},
        {title: 'Ứng viên', className: 'text-center', render: function (j) {
            return $('<span>').text(j.activeCandidateCount + ' đang xử lý / ' + j.candidateCount);
        }},
        {title: 'Mức lương', className: 'small', render: salary},
        {title: 'Trạng thái', sortKey: 'status', render: function (j) { return HRM.badge('jobStatus', j.status); }},
        {title: 'Hạn nộp', sortKey: 'openDate', className: 'small', render: function (j) { return HRM.fmt.date(j.closeDate); }},
        {title: '', className: 'table-actions', render: function (j) {
            var $g = $('<div class="btn-group btn-group-sm">')
                .append($('<a class="btn btn-outline-primary" title="Pipeline ứng viên">')
                    .attr('href', HRM.url('/recruitment/jobs/' + j.jobPostingId)).append('<i class="bi bi-kanban"></i>'));
            if (canEdit) {
                $g.append($('<button class="btn btn-outline-secondary act-edit" title="Sửa">').append('<i class="bi bi-pencil"></i>'));
                var $dd = $('<div class="btn-group btn-group-sm">').append(
                    $('<button class="btn btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown" title="Đổi trạng thái">')
                        .append('<i class="bi bi-flag"></i>'));
                var $menu = $('<ul class="dropdown-menu dropdown-menu-end">');
                $.each([['OPEN', 'Mở tuyển'], ['ON_HOLD', 'Tạm dừng'], ['CLOSED', 'Đóng tin']], function (_, s) {
                    if (s[0] !== j.status && j.status !== 'CLOSED') {
                        $menu.append($('<li>').append($('<a class="dropdown-item act-status" href="#">').attr('data-status', s[0]).text(s[1])));
                    }
                });
                if ($menu.children().length) {
                    $g.append($dd.append($menu));
                }
                $g.append($('<button class="btn btn-outline-danger act-delete" title="Xóa">').append('<i class="bi bi-trash"></i>'));
            }
            return $g;
        }}
    ];

    var table = new HRM.DataTable($('#jobTable'), {url: '/api/jobs', filterForm: '#filterForm', columns: columns, sort: 'openDate', dir: 'desc'});
    table.reload();

    HRM.get('/api/jobs/stats').done(function (s) {
        var items = [['openJobs', 'Tin đang tuyển', 'megaphone', 'primary'], ['openPositions', 'Vị trí cần tuyển', 'briefcase', 'info'],
            ['activeCandidates', 'Hồ sơ đang xử lý', 'people', 'warning'], ['interviewing', 'Đang phỏng vấn', 'chat-dots', 'success']];
        var $stats = $('#stats').empty();
        $.each(items, function (_, it) {
            $stats.append($('<div class="col-6 col-xl-3">').append($('<div class="card stat-card h-100">').append(
                $('<div class="card-body d-flex gap-3 align-items-center">').append(
                    $('<div class="stat-icon">').addClass('bg-soft-' + it[3]).append($('<i class="bi">').addClass('bi-' + it[2])),
                    $('<div>').append($('<div class="stat-value">').text(s[it[0]] || 0), $('<div class="stat-label">').text(it[1]))))));
        });
    });

    if (!canEdit) {
        return;
    }
    function open(job) {
        HRM.form.fill($form, job);
        $form.find('.modal-title').text(job.jobPostingId ? 'Sửa tin tuyển dụng' : 'Tạo tin tuyển dụng');
        HRM.modal('#jobModal').show();
    }
    $('#btnAdd').on('click', function () {
        open({openings: 1, status: 'DRAFT', employmentType: 'FULL_TIME', location: 'Hà Nội'});
    });
    $('#jobTable').on('click', '.act-edit', function () {
        open($(this).closest('tr').data('row'));
    }).on('click', '.act-status', function (e) {
        e.preventDefault();
        var j = $(this).closest('tr').data('row');
        var status = $(this).data('status');
        HRM.post('/api/jobs/' + j.jobPostingId + '/status', {status: status}).done(function () {
            HRM.toast('Đã chuyển tin ' + j.jobCode + ' sang "' + HRM.label('jobStatus', status) + '"');
            table.reload();
        });
    }).on('click', '.act-delete', function () {
        var j = $(this).closest('tr').data('row');
        HRM.confirm('Xóa tin tuyển dụng ' + j.jobCode + '?', {variant: 'danger', okText: 'Xóa'}).done(function () {
            HRM.del('/api/jobs/' + j.jobPostingId).done(function () {
                HRM.toast('Đã xóa tin tuyển dụng.');
                table.reload();
            });
        });
    });
    $form.on('submit', function (e) {
        e.preventDefault();
        var min = Number($('#jMin').val() || 0);
        var max = $('#jMax').val();
        $('#jMax')[0].setCustomValidity(max && Number(max) < min ? '≥ lương từ' : '');
        if (!HRM.form.validate($form) || !$form[0].checkValidity()) {
            return;
        }
        var data = HRM.form.toObject($form);
        var id = data.jobPostingId;
        delete data.jobPostingId;
        (id ? HRM.put('/api/jobs/' + id, data, {form: $form}) : HRM.post('/api/jobs', data, {form: $form})).done(function () {
            HRM.modal('#jobModal').hide();
            HRM.toast('Đã lưu tin tuyển dụng.');
            table.reload();
        });
    });
});
