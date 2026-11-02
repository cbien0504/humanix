/* Landing page: AJAX call to the health endpoint */
$(function () {
    $('#btnHealth').on('click', function () {
        $.getJSON(HRM.url('/health'))
            .always(function (data, status, xhr) {
                var body = xhr && xhr.responseJSON ? xhr.responseJSON : (data.responseJSON || data);
                $('#healthResult').prop('hidden', false).text(JSON.stringify(body, null, 2));
            });
    });
});
