/* Payroll parameters (ADMIN) */
$(function () {
    'use strict';
    var $form = $('#settingsForm');

    function load() {
        HRM.get('/api/payroll/settings').done(function (data) {
            var $f = $('#settingFields').empty();
            $.each(data.settings, function (_, s) {
                var id = 's_' + s.key;
                $f.append($('<div class="mb-3">').append(
                    $('<label class="form-label">').attr('for', id).text(s.description),
                    $('<input type="number" class="form-control" step="any" min="0" required data-type="number">')
                        .attr({id: id, name: s.key}).val(s.value),
                    $('<div class="form-text font-monospace">').text(s.key),
                    $('<div class="invalid-feedback">').text('Giá trị không hợp lệ.')));
            });
            var $b = $('#brackets').empty();
            $.each(data.taxBrackets, function (_, t) {
                $b.append($('<tr>').append($('<td>').text(t.bracketNo), $('<td class="money">').text(HRM.fmt.money(t.lowerBound)),
                    $('<td class="money">').text(t.upperBound === null ? '∞' : HRM.fmt.money(t.upperBound)),
                    $('<td class="text-end">').text(HRM.fmt.number(t.rate * 100) + '%')));
            });
        });
    }

    $form.on('submit', function (e) {
        e.preventDefault();
        if (!HRM.form.validate($form)) {
            return;
        }
        HRM.put('/api/payroll/settings', HRM.form.toObject($form), {form: $form}).done(function () {
            HRM.toast('Đã lưu tham số lương.');
            load();
        });
    });
    load();
});
