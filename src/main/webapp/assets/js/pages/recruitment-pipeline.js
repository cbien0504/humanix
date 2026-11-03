$(function () {
    'use strict';
    var $page = $('#pipelinePage');
    var jobId = $page.data('job-id');
    var canEdit = $page.data('can-edit') === true;
    var current = null;
    var COLUMNS = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'CLOSED_OUT'];
    var SOURCES = {WEBSITE: 'Website', LINKEDIN: 'LinkedIn', TOPCV: 'TopCV', VIETNAMWORKS: 'VietnamWorks', REFERRAL: 'Giới thiệu', OTHER: 'Khác'};

    function stars(n) {
        return n ? '★'.repeat(n) + '☆'.repeat(5 - n) : '';
    }

    function card(c) {
        var $c = $('<div class="pipeline-card">').data('id', c.candidateId)
            .append($('<div class="fw-semibold">').text(c.fullName))
            .append($('<div class="small text-body-secondary">').text(SOURCES[c.source] || c.source));
        if (c.rating) {
            $c.append($('<div class="small text-warning">').text(stars(c.rating)));
        }
        if (c.interviewAt && c.stage === 'INTERVIEW') {
            $c.append($('<div class="small"><i class="bi bi-calendar-event me-1"></i></div>').append(document.createTextNode(HRM.fmt.localDateTime(c.interviewAt))));
        }
        if (c.stage === 'REJECTED' || c.stage === 'WITHDRAWN') {
            $c.append(HRM.badge('candidateStage', c.stage));
        }
        $c.append($('<div class="small text-body-secondary">').text('Nộp ' + HRM.fmt.date(c.appliedDate)));
        return $c;
    }

    function load() {
        HRM.get('/api/jobs/' + jobId + '/pipeline').done(function (p) {
            var $board = $('#board').empty();
            var cols = p.columns;
            cols.CLOSED_OUT = (cols.REJECTED || []).concat(cols.WITHDRAWN || []);
            $.each(COLUMNS, function (_, key) {
                var list = cols[key] || [];
                var title = key === 'CLOSED_OUT' ? 'Từ chối / Rút' : HRM.label('candidateStage', key);
                var $col = $('<div class="pipeline-column">').append($('<h6>').text(title).append($('<span class="badge text-bg-light">').text(list.length)));
                $.each(list, function (_, c) { $col.append(card(c)); });
                $board.append($col);
            });
            $('#hiredCount').text((cols.HIRED || []).length);
        });
    }

    function openCandidate(id) {
        HRM.get('/api/candidates/' + id).done(function (d) {
            var c = d.candidate;
            current = c;
            $('#cTitle').text(c.fullName + ' – ' + HRM.label('candidateStage', c.stage));
            var $info = $('#cInfo').empty();
            function row(label, value) {
                $info.append($('<dt class="col-5">').text(label), $('<dd class="col-7">').append(value));
            }
            row('Email', document.createTextNode(c.email || ''));
            row('Điện thoại', document.createTextNode(c.phone || ''));
            row('Nguồn', document.createTextNode(SOURCES[c.source] || c.source));
            row('Lương mong muốn', document.createTextNode(HRM.fmt.money(c.expectedSalary)));
            row('Ngày nộp', document.createTextNode(HRM.fmt.date(c.appliedDate)));
            if (c.resumeUrl && /^https?:\/\//i.test(c.resumeUrl)) {
                row('CV', $('<a target="_blank" rel="noopener noreferrer">').attr('href', c.resumeUrl).text('Mở CV'));
            }
            var $h = $('#cHistory').empty();
            $.each(d.history, function (_, s) {
                $h.append($('<li class="mb-1">').append($('<i class="bi bi-arrow-right-circle me-1 text-primary">'))
                    .append(document.createTextNode((s.fromStage ? HRM.label('candidateStage', s.fromStage) + ' → ' : '')
                        + HRM.label('candidateStage', s.toStage) + ' · ' + HRM.fmt.instant(s.changedAt) + ' · ' + (s.changedBy || ''))));
            });
            HRM.form.fill($('#reviewForm'), {rating: c.rating, interviewAt: c.interviewAt ? c.interviewAt.substring(0, 16) : '', notes: c.notes});
            var $actions = $('#stageActions').empty();
            if (canEdit && !c.stage.match(/HIRED|REJECTED|WITHDRAWN/)) {
                var $left = $('<div class="d-flex gap-2 flex-wrap">');
                $.each(d.nextStages, function (_, s) {
                    var variant = s === 'REJECTED' ? 'outline-danger' : s === 'WITHDRAWN' ? 'outline-secondary' : 'primary';
                    $left.append($('<button class="btn btn-sm act-stage">').addClass('btn-' + variant).attr('data-stage', s)
                        .text(s === 'REJECTED' ? 'Từ chối' : s === 'WITHDRAWN' ? 'Rút hồ sơ' : '→ ' + HRM.label('candidateStage', s)));
                });
                if (c.stage === 'OFFER') {
                    $left.append($('<button class="btn btn-sm btn-success act-hire">').append('<i class="bi bi-person-check me-1"></i>Tiếp nhận'));
                }
                $actions.append($left);
            }
            if (canEdit && c.stage !== 'HIRED') {
                $actions.append($('<button class="btn btn-sm btn-outline-danger act-remove">').append('<i class="bi bi-trash me-1"></i>Xóa hồ sơ'));
            }
            HRM.modal('#candidateModal').show();
        });
    }

    $('#board').on('click', '.pipeline-card', function () {
        openCandidate($(this).data('id'));
    });

    $('#reviewForm').on('submit', function (e) {
        e.preventDefault();
        var data = $.extend({}, current, HRM.form.toObject($(this)));
        HRM.put('/api/candidates/' + current.candidateId, data, {form: $(this)}).done(function () {
            HRM.toast('Đã lưu nhận xét.');
            load();
        });
    });

    $('#stageActions').on('click', '.act-stage', function () {
        var stage = $(this).data('stage');
        HRM.post('/api/candidates/' + current.candidateId + '/stage', {stage: stage}).done(function () {
            HRM.toast(current.fullName + ' → ' + HRM.label('candidateStage', stage));
            HRM.modal('#candidateModal').hide();
            load();
        });
    }).on('click', '.act-remove', function () {
        HRM.confirm('Xóa hồ sơ ứng viên ' + current.fullName + '?', {variant: 'danger', okText: 'Xóa'}).done(function () {
            HRM.del('/api/candidates/' + current.candidateId).done(function () {
                HRM.modal('#candidateModal').hide();
                HRM.toast('Đã xóa hồ sơ.');
                load();
            });
        });
    }).on('click', '.act-hire', function () {
        $.when(HRM.get('/api/employees/next-code'), HRM.get('/api/employees/options')).done(function (code, managers) {
            HRM.fillSelect($('#hManager'), managers[0], 'employeeId', function (o) { return o.employeeCode + ' – ' + o.fullName; }, '— Trưởng phòng —');
            HRM.form.fill($('#hireForm'), {employeeCode: code[0].employeeCode, gender: 'M', baseSalary: current.expectedSalary,
                hireDate: new Date().toISOString().substring(0, 10)});
            HRM.modal('#candidateModal').hide();
            HRM.modal('#hireModal').show();
        });
    });

    $('#hireForm').on('submit', function (e) {
        e.preventDefault();
        var $f = $(this);
        if (!HRM.form.validate($f)) {
            return;
        }
        HRM.post('/api/candidates/' + current.candidateId + '/hire', HRM.form.toObject($f), {form: $f}).done(function (emp) {
            HRM.modal('#hireModal').hide();
            HRM.toast('Đã tiếp nhận ' + emp.fullName + ' – mã nhân viên ' + emp.employeeCode + ' (thử việc).');
            load();
        });
    });

    $('#btnAddCandidate').on('click', function () {
        HRM.form.fill($('#addForm'), {source: 'WEBSITE'});
        HRM.modal('#addModal').show();
    });
    $('#addForm').on('submit', function (e) {
        e.preventDefault();
        var $f = $(this);
        if (!HRM.form.validate($f)) {
            return;
        }
        var data = HRM.form.toObject($f);
        data.jobPostingId = jobId;
        HRM.post('/api/candidates', data, {form: $f}).done(function (c) {
            HRM.modal('#addModal').hide();
            HRM.toast('Đã thêm ứng viên ' + c.fullName);
            load();
        });
    });

    load();
});
