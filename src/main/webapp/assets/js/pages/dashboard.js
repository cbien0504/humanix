$(function () {
    'use strict';
    var WEEKDAYS = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];

    function renderMe(me) {
        var a = me.attendance;
        var text = me.onLeave ? 'Đang nghỉ phép' : (!a || !a.checkIn ? 'Chưa check-in' : 'Vào ' + HRM.fmt.time(a.checkIn)
            + (a.checkOut ? ' · Ra ' + HRM.fmt.time(a.checkOut) : '') + ' · ' + HRM.label('attendanceStatus', a.status));
        $('#meAttendance').text(text);
        $('#meLeave').text(me.annualLeaveAvailable !== undefined ? HRM.fmt.number(me.annualLeaveAvailable) + ' ngày' : '—');
        if (me.lastPayslip) {
            $('#mePayslipLabel').text('Lương thực lĩnh kỳ ' + me.lastPayslip.period);
            $('#mePayslip').text(HRM.fmt.money(me.lastPayslip.netSalary) + ' ₫');
            $('#mePayslipLink').attr('href', HRM.url('/payroll/payslip/' + me.lastPayslip.payrollId));
        }
        var $todo = $('#meTodo').empty();
        if (me.pendingSelfReviews > 0) {
            $todo.append($('<li>').append($('<a>').attr('href', HRM.url('/performance')).text(me.pendingSelfReviews + ' phiếu tự đánh giá cần hoàn thành')));
        }
        if (me.pendingLeaveRequests > 0) {
            $todo.append($('<li>').append($('<a>').attr('href', HRM.url('/leave')).text(me.pendingLeaveRequests + ' đơn nghỉ đang chờ duyệt')));
        }
        if (!$todo.children().length) {
            $todo.append($('<li class="text-body-secondary">').text('Không có việc tồn đọng.'));
        }
        $('#meSection').prop('hidden', false);
    }

    function renderTeam(t) {
        $('#teamCheckin').text((t.checkedIn || 0) + '/' + (t.teamSize || 0));
        $('#teamApprovals').text(t.pendingApprovals);
        $('#teamReviews').text(t.reviewsToScore);
        $('#teamSection').prop('hidden', false);
    }

    function renderCompany(c) {
        var h = c.headcount;
        var a = c.attendanceToday;
        $('#kHeadcount').text(h.headcount);
        $('#kHeadcountSub').text('+' + h.newHiresMonth + ' tuyển mới trong tháng · ' + h.leaversYtd + ' nghỉ việc năm nay');
        $('#kCheckin').text(a.checkedIn + '/' + a.expected);
        $('#kCheckinSub').text(a.late + ' đi muộn · ' + a.onLeave + ' nghỉ phép');
        $('#kPending').text(c.pendingLeaveRequests);
        $('#kOnLeave').text('cần xử lý');
        $('#kJobs').text(c.recruitment.openJobs + ' tin đang mở');
        $('#kJobsSub').text(c.recruitment.activeCandidates + ' hồ sơ đang xử lý · ' + c.recruitment.interviewing + ' phỏng vấn');
        $('#companySection').prop('hidden', false);

        var C = HRM.charts;
        var S = C.tokens.series;

        var labels = c.attendanceTrend.map(function (d) { return HRM.fmt.date(d.date).substring(0, 5); });
        var keys = [['onTime', 'Đúng giờ'], ['late', 'Đi muộn'], ['halfDay', 'Nửa ngày']];
        new Chart(document.getElementById('attendanceChart'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: keys.map(function (k, i) {
                    var top = i === keys.length - 1;
                    return {
                        label: k[1], data: c.attendanceTrend.map(function (d) { return d[k[0]]; }),
                        backgroundColor: S[i], maxBarThickness: 24, stack: 'day',
                        borderColor: C.tokens.surface,
                        borderWidth: i === 0 ? 0 : {bottom: 2, top: 0, left: 0, right: 0},
                        borderSkipped: i === 0 ? 'start' : false,
                        borderRadius: top ? {topLeft: 4, topRight: 4} : 0
                    };
                })
            },
            options: {
                interaction: {mode: 'index', intersect: false},
                scales: $.extend(true, C.axes(false), {x: {stacked: true}, y: {stacked: true}}),
                plugins: {
                    legend: {position: 'top'},
                    tooltip: {callbacks: {label: function (ctx) { return ctx.parsed.y + '  ' + ctx.dataset.label; }}}
                }
            }
        });
        C.tableView($('#attendanceTable'), ['Ngày', 'Đúng giờ', 'Đi muộn', 'Nửa ngày'],
            c.attendanceTrend.map(function (d) { return [HRM.fmt.date(d.date), d.onTime, d.late, d.halfDay]; }));

        new Chart(document.getElementById('departmentChart'), {
            type: 'bar',
            data: {
                labels: c.departments.map(function (d) { return d.name.replace(/^Phòng /, ''); }),
                datasets: [{label: 'Nhân viên', data: c.departments.map(function (d) { return d.headcount; }),
                    backgroundColor: S[0], maxBarThickness: 24, borderRadius: 4, borderSkipped: 'start'}]
            },
            options: {
                indexAxis: 'y',
                layout: {padding: {right: 28}},
                scales: $.extend(true, C.axes(true), {x: {display: false}}),
                plugins: {
                    legend: {display: false},
                    valueLabels: {format: function (v) { return String(v); }},
                    tooltip: {callbacks: {label: function (ctx) { return ctx.parsed.x + ' nhân viên'; }}}
                }
            }
        });
        C.tableView($('#departmentTable'), ['Phòng ban', 'Nhân viên'], c.departments.map(function (d) { return [d.name, d.headcount]; }));

        C.segmentedBar($('#genderBar'), [{label: 'Nam', value: h.male}, {label: 'Nữ', value: h.female}, {label: 'Khác', value: h.other}]);
        C.segmentedBar($('#statusBar'), [{label: 'Chính thức', value: h.active}, {label: 'Thử việc', value: h.probation},
            {label: 'Nghỉ dài hạn', value: h.onLongLeave}]);

        var periods = c.payrollTrend;
        new Chart(document.getElementById('payrollChart'), {
            type: 'bar',
            data: {
                labels: periods.map(function (p) { return String(p.month).padStart(2, '0') + '/' + p.year; }),
                datasets: [{label: 'Thực lĩnh', data: periods.map(function (p) { return Number(p.totalNet); }),
                    backgroundColor: S[0], maxBarThickness: 24, borderRadius: 4, borderSkipped: 'start'}]
            },
            options: {
                layout: {padding: {top: 20}},
                scales: C.axes(false, {ticks: {callback: function (v) { return C.moneyCompact(v); }}}),
                plugins: {
                    legend: {display: false},
                    valueLabels: {format: function (v) { return C.moneyCompact(v); }},
                    tooltip: {callbacks: {label: function (ctx) {
                        var p = periods[ctx.dataIndex];
                        return HRM.fmt.money(ctx.parsed.y) + ' ₫ · ' + p.employees + ' NV · ' + HRM.label('payrollStatus', p.status);
                    }}}
                }
            }
        });
        C.tableView($('#payrollTable'), ['Kỳ', 'Tổng thu nhập', 'Thực lĩnh', 'Số NV'], periods.map(function (p) {
            return [String(p.month).padStart(2, '0') + '/' + p.year, HRM.fmt.money(p.totalGross), HRM.fmt.money(p.totalNet), p.employees];
        }));
    }

    HRM.get('/api/dashboard').done(function (d) {
        var p = String(d.today).split('-');
        var day = new Date(+p[0], p[1] - 1, +p[2]);
        $('#todayLabel').text(WEEKDAYS[day.getDay()] + ', ' + HRM.fmt.date(d.today));
        if (d.me) {
            renderMe(d.me);
        }
        if (d.team) {
            renderTeam(d.team);
        }
        if (d.company && window.Chart) {
            renderCompany(d.company);
        }
    });
});
