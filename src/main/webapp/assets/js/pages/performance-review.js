/* Self-assessment / manager scoring form (UC-51, UC-52) */
$(function () {
    'use strict';
    var $page = $('#reviewPage');
    var mode = $page.data('mode');
    var id = $page.data('review-id');
    var $form = $('#scoreForm');
    if (mode === 'readonly') {
        return;
    }
    $form.on('submit', function (e) {
        e.preventDefault();
        if (!HRM.form.validate($form)) {
            HRM.toast('Vui lòng chấm điểm đủ tất cả tiêu chí.', 'warning');
            return;
        }
        var scores = {};
        $form.find('[data-criteria]').each(function () {
            scores[$(this).data('criteria')] = Number($(this).val());
        });
        var comment = $form.find('textarea[name=comment]').val();
        HRM.confirm('Gửi phiếu đánh giá? Sau khi gửi bạn không thể sửa.', {okText: 'Gửi'}).done(function () {
            HRM.post('/api/reviews/' + id + '/' + mode, {scores: scores, comment: comment}, {form: $form}).done(function (r) {
                HRM.toast(mode === 'self' ? 'Đã gửi tự đánh giá (điểm ' + HRM.fmt.number(r.selfScore, 2) + ').'
                    : 'Đã hoàn thành đánh giá – điểm cuối ' + HRM.fmt.number(r.finalScore, 2) + ', xếp loại ' + r.rating + '.');
                setTimeout(function () { window.location.reload(); }, 900);
            });
        });
    });
});
