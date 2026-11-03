$(function () {
    'use strict';
    var $form = $('#posForm');

    function load() {
        HRM.get('/api/positions').done(function (list) {
            var $rows = $('#rows').empty();
            $.each(list, function (_, p) {
                $rows.append($('<tr>').data('row', p).append(
                    $('<td class="fw-semibold">').text(p.positionCode),
                    $('<td>').text(p.positionName),
                    $('<td class="text-center">').text(p.jobLevel),
                    $('<td class="money">').text(HRM.fmt.money(p.minSalary)),
                    $('<td class="money">').text(HRM.fmt.money(p.maxSalary)),
                    $('<td class="text-end">').text(p.employeeCount),
                    $('<td>').append(p.active ? $('<span class="badge text-bg-success">').text('Đang dùng')
                        : $('<span class="badge text-bg-secondary">').text('Ngừng')),
                    $('<td class="table-actions">').append($('<div class="btn-group btn-group-sm">')
                        .append($('<button class="btn btn-outline-primary act-edit" title="Sửa">').append('<i class="bi bi-pencil"></i>'))
                        .append($('<button class="btn btn-outline-danger act-delete" title="Xóa">').append('<i class="bi bi-trash"></i>')))));
            });
        });
    }

    function open(p) {
        HRM.form.fill($form, p);
        $form.find('.modal-title').text(p.positionId ? 'Sửa chức danh' : 'Thêm chức danh');
        HRM.modal('#posModal').show();
    }

    $('#btnAdd').on('click', function () {
        open({jobLevel: 3, minSalary: 0, active: true});
    });
    $('#rows').on('click', '.act-edit', function () {
        open($(this).closest('tr').data('row'));
    }).on('click', '.act-delete', function () {
        var p = $(this).closest('tr').data('row');
        HRM.confirm('Xóa chức danh "' + p.positionName + '"?', {variant: 'danger', okText: 'Xóa'}).done(function () {
            HRM.del('/api/positions/' + p.positionId).done(function () {
                HRM.toast('Đã xóa chức danh.');
                load();
            });
        });
    });

    $form.on('submit', function (e) {
        e.preventDefault();
        var min = Number($('#pMin').val());
        $('#pMax')[0].setCustomValidity(Number($('#pMax').val()) < min ? 'Phải ≥ lương tối thiểu' : '');
        if (!HRM.form.validate($form) || $('#pMax')[0].validationMessage) {
            $form.addClass('was-validated');
            return;
        }
        var data = HRM.form.toObject($form);
        var id = data.positionId;
        delete data.positionId;
        (id ? HRM.put('/api/positions/' + id, data, {form: $form}) : HRM.post('/api/positions', data, {form: $form}))
            .done(function () {
                HRM.modal('#posModal').hide();
                HRM.toast('Đã lưu chức danh.');
                load();
            });
    });

    load();
});
