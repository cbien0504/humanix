/* Data integrity checks (UC-06) */
$(function () {
    'use strict';
    $('#btnRun').on('click', function () {
        var $btn = $(this).prop('disabled', true);
        $('#checks').html('<tr><td colspan="6"><div class="spinner-overlay"><div class="spinner-border text-primary"></div></div></td></tr>');
        HRM.get('/api/admin/data-validation').done(function (checks) {
            var counts = {PASS: 0, WARN: 0, FAIL: 0};
            var $body = $('#checks').empty();
            $.each(checks, function (_, c) {
                counts[c.result] = (counts[c.result] || 0) + 1;
                $body.append($('<tr>').append(
                    $('<td class="fw-semibold">').text(c.checkCode),
                    $('<td>').text(c.checkName),
                    $('<td>').append($('<span class="badge">').addClass(c.severity === 'ERROR' ? 'text-bg-danger' : 'text-bg-warning').text(c.severity)),
                    $('<td class="text-end">').text(c.violationCount),
                    $('<td>').append(HRM.badge('validation', c.result)),
                    $('<td class="small text-body-secondary">').text(c.sampleKeys || '')
                ));
            });
            $('#cntPass').text(counts.PASS);
            $('#cntWarn').text(counts.WARN);
            $('#cntFail').text(counts.FAIL);
            $('#summary').prop('hidden', false);
            HRM.toast(counts.FAIL ? 'Có ' + counts.FAIL + ' kiểm tra lỗi!' : 'Dữ liệu đạt tất cả kiểm tra lỗi nghiêm trọng.', counts.FAIL ? 'danger' : 'success');
        }).always(function () {
            $btn.prop('disabled', false);
        });
    });
});
