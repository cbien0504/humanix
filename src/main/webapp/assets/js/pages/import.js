$(function () {
    'use strict';
    var lastResult = null;
    var MODES = {VALIDATE_ONLY: 'Chỉ kiểm tra', VALID_ONLY: 'Import dòng hợp lệ', ALL_OR_NOTHING: 'Tất cả hoặc không'};

    $('#importForm').on('submit', function (e) {
        e.preventDefault();
        var $form = $(this);
        var file = $('#file')[0].files[0];
        $('#file')[0].setCustomValidity(file && file.size > 5 * 1024 * 1024 ? 'Tối đa 5 MB' : '');
        if (!HRM.form.validate($form)) {
            return;
        }
        var data = new FormData();
        data.append('file', file);
        data.append('mode', $form.find('[name=mode]:checked').val());
        var $btn = $('#btnUpload').prop('disabled', true);
        $.ajax({url: HRM.url('/import/employees'), method: 'POST', data: data, processData: false, contentType: false, dataType: 'json'})
            .done(render)
            .fail(function (xhr) {
                HRM.toast(HRM.errorMessage(xhr), 'danger');
            })
            .always(function () {
                $btn.prop('disabled', false);
            });
    });

    function render(r) {
        lastResult = r;
        $('#resultTitle').text('Kết quả: ' + r.fileName + ' (' + MODES[r.mode] + ')');
        $('#rTotal').text(r.totalRows);
        $('#rValid').text(r.validRows);
        $('#rInvalid').text(new Set(r.errors.map(function (x) { return x.row; })).size);
        $('#rImported').text(r.importedRows);
        var $rows = $('#errorRows').empty();
        if (!r.errors.length) {
            $rows.append($('<tr>').append($('<td colspan="4" class="text-success">').text('Không có lỗi.')));
        }
        $.each(r.errors, function (_, x) {
            $rows.append($('<tr>').append($('<td>').text(x.row), $('<td>').append($('<code>').text(x.column)),
                $('<td class="small">').text(x.value), $('<td class="small">').text(x.message)));
        });
        $('#btnErrors').prop('hidden', !r.errors.length);
        $('#resultCard').prop('hidden', false);
        HRM.toast(r.importedRows ? 'Đã import ' + r.importedRows + ' nhân viên.' : (r.errors.length ? 'Có ' + r.errors.length + ' lỗi cần sửa.'
            : 'Dữ liệu hợp lệ – có thể import.'), r.errors.length ? 'warning' : 'success');
    }

    $('#btnErrors').on('click', function () {
        if (!lastResult) {
            return;
        }
        function csv(v) {
            var s = String(v === null || v === undefined ? '' : v);
            if (/^[=+\-@\t\r]/.test(s)) {
                s = "'" + s;
            }
            return '"' + s.replace(/"/g, '""') + '"';
        }
        var lines = ['row,column,value,message'].concat(lastResult.errors.map(function (x) {
            return [x.row, x.column, x.value, x.message].map(csv).join(',');
        }));
        var blob = new Blob(['﻿' + lines.join('\r\n')], {type: 'text/csv;charset=utf-8'});
        var a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'loi-import-' + lastResult.fileName.replace(/\.[^.]+$/, '') + '.csv';
        document.body.appendChild(a);
        a.click();
        a.remove();
    });
});
