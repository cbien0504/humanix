/* Print button (inline onclick handlers are blocked by the CSP) */
$(function () {
    'use strict';
    $('#btnPrint').on('click', function () {
        window.print();
    });
});
