/* Departments CRUD (UC-10) */
$(function () {
    'use strict';
    var $form = $('#deptForm');

    function actions() {
        return $('<div class="btn-group btn-group-sm">')
            .append($('<button class="btn btn-outline-primary act-edit" title="Sửa">').append('<i class="bi bi-pencil"></i>'))
            .append($('<button class="btn btn-outline-danger act-delete" title="Xóa">').append('<i class="bi bi-trash"></i>'));
    }

    function load() {
        HRM.get('/api/departments').done(function (list) {
            var $rows = $('#rows').empty();
            $.each(list, function (_, d) {
                $rows.append($('<tr>').data('row', d).append(
                    $('<td class="fw-semibold">').text(d.departmentCode),
                    $('<td>').text(d.departmentName).append($('<div class="small text-body-secondary">').text(d.description || '')),
                    $('<td>').text(d.managerName || '—'),
                    $('<td class="text-end">').text(d.employeeCount),
                    $('<td>').append(d.active ? $('<span class="badge text-bg-success">').text('Hoạt động')
                        : $('<span class="badge text-bg-secondary">').text('Ngừng')),
                    $('<td class="table-actions">').append(actions())));
            });
        });
    }

    function open(dept) {
        HRM.get('/api/employees/options').done(function (list) {
            HRM.fillSelect($('#dManager'), list, 'employeeId', function (o) {
                return o.employeeCode + ' – ' + o.fullName;
            }, '— Chưa chỉ định —');
            HRM.form.fill($form, dept);
            $form.find('.modal-title').text(dept.departmentId ? 'Sửa phòng ban' : 'Thêm phòng ban');
            HRM.modal('#deptModal').show();
        });
    }

    $('#btnAdd').on('click', function () {
        open({active: true});
    });
    $('#rows').on('click', '.act-edit', function () {
        open($(this).closest('tr').data('row'));
    }).on('click', '.act-delete', function () {
        var d = $(this).closest('tr').data('row');
        HRM.confirm('Xóa phòng ban "' + d.departmentName + '"?', {variant: 'danger', okText: 'Xóa'}).done(function () {
            HRM.del('/api/departments/' + d.departmentId).done(function () {
                HRM.toast('Đã xóa phòng ban.');
                load();
            });
        });
    });

    $form.on('submit', function (e) {
        e.preventDefault();
        if (!HRM.form.validate($form)) {
            return;
        }
        var data = HRM.form.toObject($form);
        var id = data.departmentId;
        delete data.departmentId;
        (id ? HRM.put('/api/departments/' + id, data, {form: $form}) : HRM.post('/api/departments', data, {form: $form}))
            .done(function () {
                HRM.modal('#deptModal').hide();
                HRM.toast('Đã lưu phòng ban.');
                load();
            });
    });

    load();
});
