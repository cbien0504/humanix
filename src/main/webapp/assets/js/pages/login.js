$(function () {
    'use strict';
    var $form = $('#loginForm');
    $form.on('submit', function (e) {
        if (!this.checkValidity()) {
            e.preventDefault();
            $form.addClass('was-validated');
        }
    });
    $('#togglePassword').on('click', function () {
        var $pw = $('#password');
        var show = $pw.attr('type') === 'password';
        $pw.attr('type', show ? 'text' : 'password');
        $(this).find('i').toggleClass('bi-eye', !show).toggleClass('bi-eye-slash', show);
    });
    $('.demo-login').on('click', function () {
        $('#username').val($(this).data('user'));
        $('#password').val('Demo@123');
        var form = $form[0];
        if (typeof form.requestSubmit === 'function') {
            form.requestSubmit();
        } else {
            form.submit();
        }
    });
});
