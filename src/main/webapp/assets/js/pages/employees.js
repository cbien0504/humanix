/* Employee list & CRUD (UC-12 … UC-14). Permissions are re-checked by the server. */
$(function () {
    'use strict';
    var canEdit = $('#employeePage').data('can-edit') === true;
    var $form = $('#employeeForm');
    var managersLoaded = false;

    var columns = [
        {title: 'Mã NV', sortKey: 'code', render: function (e) {
            return $('<span class="fw-semibold">').text(e.employeeCode);
        }},
        {title: 'Họ tên', sortKey: 'name', render: function (e) {
            return $('<div>')
                .append($('<a>').attr('href', HRM.url('/employees/' + e.employeeId)).text(e.fullName))
                .append($('<div class="small text-body-secondary">').text(e.email));
        }},
        {title: 'Phòng ban', key: 'departmentName', sortKey: 'department'},
        {title: 'Chức danh', key: 'positionName', sortKey: 'position'},
        {title: 'Ngày vào làm', sortKey: 'hireDate', render: function (e) {
            return HRM.fmt.date(e.hireDate);
        }},
        {title: 'Trạng thái', sortKey: 'status', render: function (e) {
            return HRM.badge('employeeStatus', e.status);
        }}
    ];
    if (canEdit) {
        columns.splice(5, 0, {title: 'Lương CB', sortKey: 'salary', className: 'money', render: function (e) {
            return HRM.fmt.money(e.baseSalary);
        }});
    }
    columns.push({title: '', className: 'table-actions', render: function (e) {
        var $g = $('<div class="btn-group btn-group-sm">');
        $g.append($('<a class="btn btn-outline-secondary" title="Xem hồ sơ">')
            .attr('href', HRM.url('/employees/' + e.employeeId)).append('<i class="bi bi-eye"></i>'));
        if (canEdit) {
            $g.append($('<button class="btn btn-outline-primary act-edit" title="Sửa">').append('<i class="bi bi-pencil"></i>'));
            $g.append($('<button class="btn btn-outline-danger act-delete" title="Xóa">').append('<i class="bi bi-trash"></i>'));
        }
        return $g;
    }});

    var table = new HRM.DataTable($('#employeeTable'), {
        url: '/api/employees', filterForm: '#filterForm', sort: 'code', columns: columns
    });
    table.reload();

    if (!canEdit) {
        return;
    }

    // ---------------------------------------------------------------- form helpers
    function loadManagers() {
        if (managersLoaded) {
            return $.Deferred().resolve().promise();
        }
        return HRM.get('/api/employees/options').done(function (list) {
            HRM.fillSelect($('#fManager'), list, 'employeeId', function (o) {
                return o.employeeCode + ' – ' + o.fullName;
            }, '— Không có —');
            managersLoaded = true;
        });
    }

    function updateSalaryBand() {
        var $opt = $('#fPosition option:selected');
        var min = $opt.data('min');
        var max = $opt.data('max');
        var $salary = $('#fSalary');
        if (min !== undefined && max !== undefined) {
            $salary.attr({min: min, max: max});
            $('#salaryBand').text('Khung lương: ' + HRM.fmt.money(min) + ' – ' + HRM.fmt.money(max));
        } else {
            $salary.attr('min', 0).removeAttr('max');
            $('#salaryBand').text('');
        }
    }

    function updateTermination() {
        var terminated = $('#fStatus').val() === 'TERMINATED';
        $('#fTerm').prop('required', terminated);
    }

    $('#fPosition').on('change', updateSalaryBand);
    $('#fStatus').on('change', updateTermination);

    function open(employee) {
        loadManagers().done(function () {
            HRM.form.fill($form, employee);
            $('#fManager option').prop('disabled', false);
            if (employee.employeeId) {
                $('#fManager option[value="' + employee.employeeId + '"]').prop('disabled', true);
            }
            $form.find('.modal-title').text(employee.employeeId
                ? 'Sửa hồ sơ: ' + employee.employeeCode + ' – ' + employee.fullName : 'Thêm nhân viên');
            updateSalaryBand();
            updateTermination();
            HRM.modal('#employeeModal').show();
        });
    }

    function edit(id) {
        HRM.get('/api/employees/' + id).done(open);
    }

    // ---------------------------------------------------------------- actions
    $('#btnAdd').on('click', function () {
        HRM.get('/api/employees/next-code').done(function (res) {
            open({employeeCode: res.employeeCode, gender: 'M', status: 'PROBATION', allowance: 0, dependents: 0,
                hireDate: new Date().toISOString().substring(0, 10)});
        });
    });

    $('#employeeTable').on('click', '.act-edit', function () {
        edit($(this).closest('tr').data('row').employeeId);
    }).on('click', '.act-delete', function () {
        var e = $(this).closest('tr').data('row');
        HRM.confirm('Xóa nhân viên ' + e.employeeCode + ' – ' + e.fullName + '? Chỉ xóa được khi chưa phát sinh dữ liệu chấm công, '
            + 'nghỉ phép, lương, đánh giá.', {variant: 'danger', okText: 'Xóa'}).done(function () {
            HRM.del('/api/employees/' + e.employeeId).done(function () {
                HRM.toast('Đã xóa nhân viên ' + e.employeeCode);
                table.reload();
            });
        });
    });

    $form.on('submit', function (ev) {
        ev.preventDefault();
        if (!HRM.form.validate($form)) {
            return;
        }
        var data = HRM.form.toObject($form);
        var id = data.employeeId;
        delete data.employeeId;
        var $btn = $form.find('[type=submit]').prop('disabled', true);
        (id ? HRM.put('/api/employees/' + id, data, {form: $form}) : HRM.post('/api/employees', data, {form: $form}))
            .done(function (saved) {
                HRM.modal('#employeeModal').hide();
                HRM.toast((id ? 'Đã cập nhật ' : 'Đã thêm ') + saved.employeeCode + ' – ' + saved.fullName);
                managersLoaded = false;
                table.reload();
            })
            .always(function () {
                $btn.prop('disabled', false);
            });
    });

    // deep link from the profile page: /employees?edit=12
    if (HRM.qs('edit')) {
        edit(HRM.qs('edit'));
    }
});
