$(function () {
    'use strict';
    var WEEKDAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    var offsetMs = 0;
    var $month = $('#month');

    function pad(n) {
        return (n < 10 ? '0' : '') + n;
    }

    function tick() {
        var d = new Date(Date.now() + offsetMs);
        $('#clock').text(pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds()));
    }

    function renderToday(t) {
        var p = t.serverTime.split(/[-T:]/);
        var server = new Date(+p[0], p[1] - 1, +p[2], +p[3], +p[4], +(p[5] || 0));
        offsetMs = server.getTime() - Date.now();
        $('#todayDate').text(WEEKDAYS[server.getDay()] + ', ' + HRM.fmt.date(t.serverTime));
        $('#schedule').text('Giờ vào làm ' + t.workStart + ' (ân hạn ' + t.graceMinutes + ' phút)');
        var r = t.record;
        $('#inTime').text(r && r.checkIn ? HRM.fmt.time(r.checkIn) : '--:--');
        $('#outTime').text(r && r.checkOut ? HRM.fmt.time(r.checkOut) : '--:--');
        var $status = $('#todayStatus').empty();
        if (t.onLeave) {
            $status.append($('<span class="badge text-bg-info">').text('Đang nghỉ phép'));
        } else if (!r || !r.checkIn) {
            $status.append($('<span class="badge text-bg-light border">').text('Chưa check-in'));
        } else {
            $status.append(HRM.badge('attendanceStatus', r.status));
            if (r.lateMinutes > 0) {
                $status.append($('<span class="small text-body-secondary ms-2">').text('muộn ' + r.lateMinutes + ' phút'));
            }
        }
        $('#btnCheckIn').prop('disabled', t.onLeave || !!(r && r.checkIn));
        $('#btnCheckOut').prop('disabled', !(r && r.checkIn));
        tick();
    }

    function loadToday() {
        return HRM.get('/api/attendance/today').done(renderToday);
    }

    function loadMonth() {
        HRM.get('/api/attendance/my', {month: $month.val()}).done(function (m) {
            $('#summary [data-k]').each(function () {
                var k = $(this).data('k');
                $(this).text(HRM.fmt.number(m.summary[k]));
            });
            var $rows = $('#records').empty();
            if (!m.records.length) {
                $rows.append($('<tr>').append($('<td colspan="8" class="empty-state">').text('Chưa có dữ liệu chấm công trong tháng.')));
            }
            $.each(m.records, function (_, r) {
                var p = r.workDate.split('-');
                var day = WEEKDAYS[new Date(+p[0], p[1] - 1, +p[2]).getDay()];
                $rows.append($('<tr>').append(
                    $('<td>').text(day + ', ' + HRM.fmt.date(r.workDate)),
                    $('<td>').text(HRM.fmt.time(r.checkIn)),
                    $('<td>').text(HRM.fmt.time(r.checkOut)),
                    $('<td class="text-end">').text(HRM.fmt.number(r.workHours, 2)),
                    $('<td>').append(HRM.badge('attendanceStatus', r.status)),
                    $('<td class="text-end">').text(r.lateMinutes || ''),
                    $('<td class="small text-body-secondary">').text(r.source),
                    $('<td class="small">').text(r.note || '')));
            });
        });
    }

    function punch(kind) {
        var $btn = $(kind === 'in' ? '#btnCheckIn' : '#btnCheckOut').prop('disabled', true);
        HRM.post('/api/attendance/check-' + kind).done(function (r) {
            HRM.toast(kind === 'in' ? 'Check-in thành công lúc ' + HRM.fmt.time(r.checkIn)
                : 'Check-out thành công lúc ' + HRM.fmt.time(r.checkOut));
            loadToday();
            loadMonth();
        }).fail(function () {
            loadToday();
        });
    }

    $('#btnCheckIn').on('click', function () { punch('in'); });
    $('#btnCheckOut').on('click', function () { punch('out'); });
    $month.on('change', loadMonth);

    loadToday().done(function (t) {
        $month.val(t.serverTime.substring(0, 7));
        loadMonth();
    });
    setInterval(tick, 1000);
});
