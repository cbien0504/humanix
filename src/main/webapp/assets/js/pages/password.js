$(function () {
    'use strict';
    var $form = $('#passwordForm');
    $form.on('submit', function (e) {
        e.preventDefault();
        if (!HRM.form.validate($form)) {
            return;
        }
        var $btn = $form.find('[type=submit]').prop('disabled', true);
        HRM.post('/profile/password', HRM.form.toObject($form), {form: $form})
            .done(function (res) {
                HRM.toast(res.message);
                setTimeout(function () {
                    window.location.href = HRM.url('/');
                }, 900);
            })
            .always(function () {
                $btn.prop('disabled', false);
            });
    });
});
