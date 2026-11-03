$(function () {
    'use strict';
    var ROLE_LABELS = {ADMIN: 'Quản trị', HR: 'Nhân sự', MANAGER: 'Quản lý', EMPLOYEE: 'Nhân viên'};
    var $form = $('#userForm');

    var table = new HRM.DataTable($('#userTable'), {
        url: '/api/users',
        filterForm: '#filterForm',
        sort: 'username',
        columns: [
            {title: 'Tên đăng nhập', sortKey: 'username', render: function (u) {
                return $('<span class="fw-semibold">').text(u.username);
            }},
            {title: 'Nhân viên', sortKey: 'fullName', render: function (u) {
                return u.employeeCode ? u.employeeCode + ' – ' + u.fullName : '—';
            }},
            {title: 'Vai trò', render: function (u) {
                var $wrap = $('<span>');
                $.each(u.roles || [], function (_, r) {
                    $wrap.append($('<span class="badge text-bg-light border me-1">').text(ROLE_LABELS[r] || r));
                });
                return $wrap;
            }},
            {title: 'Trạng thái', render: function (u) {
                if (!u.active) {
                    return $('<span class="badge text-bg-secondary">').text('Vô hiệu hóa');
                }
                if (u.lockedUntil && new Date(u.lockedUntil) > new Date()) {
                    return $('<span class="badge text-bg-danger">').text('Tạm khóa');
                }
                return $('<span class="badge text-bg-success">').text('Hoạt động');
            }},
            {title: 'Đăng nhập gần nhất', sortKey: 'lastLogin', render: function (u) {
                return HRM.fmt.instant(u.lastLoginAt) || 'Chưa đăng nhập';
            }},
            {title: '', className: 'table-actions', render: function (u) {
                var $g = $('<div class="btn-group btn-group-sm">');
                $g.append($('<button class="btn btn-outline-primary act-edit" title="Sửa">').append('<i class="bi bi-pencil"></i>'));
                $g.append($('<button class="btn btn-outline-warning act-reset" title="Đặt lại mật khẩu">').append('<i class="bi bi-key"></i>'));
                if (u.lockedUntil && new Date(u.lockedUntil) > new Date()) {
                    $g.append($('<button class="btn btn-outline-success act-unlock" title="Mở khóa">').append('<i class="bi bi-unlock"></i>'));
                }
                return $g;
            }}
        ]
    });
    table.reload();

    function roles() {
        return $('#roleChecks input:checked').map(function () {
            return this.value;
        }).get();
    }

    function openModal(user) {
        HRM.form.fill($form, user || {active: true});
        $('#roleChecks input').prop('checked', false);
        $('#roleError').addClass('d-none');
        $.each((user && user.roles) || ['EMPLOYEE'], function (_, r) {
            $('#roleChecks input[value="' + r + '"]').prop('checked', true);
        });
        $form.find('.create-only').toggle(!user);
        $form.find('.edit-only').toggle(!!user);
        $form.find('#fUsername').prop('required', !user);
        $form.find('.modal-title').text(user ? 'Sửa tài khoản: ' + user.username : 'Tạo tài khoản');
        HRM.modal('#userModal').show();
    }

    function showPassword(credentials) {
        $('#pwUser').text(credentials.user.username);
        $('#pwValue').val(credentials.temporaryPassword);
        HRM.modal('#passwordModal').show();
    }

    $('#btnCreate').on('click', function () {
        HRM.get('/api/users/eligible-employees').done(function (list) {
            HRM.fillSelect($('#fEmployee'), list, 'employeeId', function (e) {
                return e.employeeCode + ' – ' + e.fullName;
            }, '— Không liên kết —');
            openModal(null);
        });
    });

    $('#userTable').on('click', '.act-edit', function () {
        openModal($(this).closest('tr').data('row'));
    }).on('click', '.act-reset', function () {
        var u = $(this).closest('tr').data('row');
        HRM.confirm('Đặt lại mật khẩu cho "' + u.username + '"? Mật khẩu hiện tại sẽ không dùng được nữa.',
            {variant: 'warning', okText: 'Đặt lại'}).done(function () {
            HRM.post('/api/users/' + u.userId + '/reset-password').done(showPassword);
        });
    }).on('click', '.act-unlock', function () {
        var u = $(this).closest('tr').data('row');
        HRM.post('/api/users/' + u.userId + '/unlock').done(function () {
            HRM.toast('Đã mở khóa tài khoản ' + u.username);
            table.reload();
        });
    });

    $form.on('submit', function (e) {
        e.preventDefault();
        var ok = HRM.form.validate($form);
        var selected = roles();
        $('#roleError').toggleClass('d-none', selected.length > 0);
        if (!ok || !selected.length) {
            return;
        }
        var data = HRM.form.toObject($form);
        var request = data.userId
            ? HRM.put('/api/users/' + data.userId, {roles: selected, active: data.active}, {form: $form})
            : HRM.post('/api/users', {username: data.username, employeeId: data.employeeId ? Number(data.employeeId) : null, roles: selected}, {form: $form});
        request.done(function (res) {
            HRM.modal('#userModal').hide();
            table.reload();
            if (res.temporaryPassword) {
                showPassword(res);
            } else {
                HRM.toast('Đã cập nhật tài khoản.');
            }
        });
    });

    $('#btnCopy').on('click', function () {
        navigator.clipboard.writeText($('#pwValue').val()).then(function () {
            HRM.toast('Đã sao chép mật khẩu tạm thời.', 'info');
        });
    });
});
