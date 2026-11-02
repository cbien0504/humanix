/*!
 * HRM chart conventions on top of Chart.js 4.
 * - Categorical palette (fixed order): blue #2a78d6, orange #eb6834, aqua #1baf7a –
 *   validated with the dataviz palette validator against the white card surface
 *   (lightness band, chroma floor, CVD ΔE 9.2, normal-vision ΔE 27.6: PASS; aqua is below
 *   3:1 contrast, so every chart ships a legend or direct labels AND a table view).
 * - Thin marks: bars ≤ 24px, 4px rounded data end, square at the baseline; 2px surface gap
 *   between stacked segments; hairline solid grid; text never wears the series colour.
 */
(function (window, $) {
    'use strict';

    var T = {
        surface: '#ffffff',
        ink: '#0b0b0b',
        ink2: '#52514e',
        muted: '#898781',
        grid: '#e1e0d9',
        axis: '#c3c2b7',
        series: ['#2a78d6', '#eb6834', '#1baf7a']
    };

    Chart.defaults.font.family = window.getComputedStyle(document.body).fontFamily;
    Chart.defaults.font.size = 12;
    Chart.defaults.color = T.muted;
    Chart.defaults.borderColor = T.grid;
    Chart.defaults.animation.duration = 250;
    Chart.defaults.maintainAspectRatio = false;
    Chart.defaults.plugins.legend.align = 'start';
    Chart.defaults.plugins.legend.labels.color = T.ink2;
    Chart.defaults.plugins.legend.labels.boxWidth = 10;
    Chart.defaults.plugins.legend.labels.boxHeight = 10;
    Chart.defaults.plugins.tooltip.backgroundColor = '#ffffff';
    Chart.defaults.plugins.tooltip.titleColor = T.ink2;
    Chart.defaults.plugins.tooltip.bodyColor = T.ink;
    Chart.defaults.plugins.tooltip.borderColor = 'rgba(11,11,11,0.10)';
    Chart.defaults.plugins.tooltip.borderWidth = 1;
    Chart.defaults.plugins.tooltip.padding = 10;
    Chart.defaults.plugins.tooltip.cornerRadius = 6;
    Chart.defaults.plugins.tooltip.usePointStyle = true;
    Chart.defaults.plugins.tooltip.bodyFont = {weight: '600'};

    /** Draws the value at the tip of each bar of dataset 0 when options.plugins.valueLabels.format is set. */
    Chart.register({
        id: 'valueLabels',
        afterDatasetsDraw: function (chart, args, opts) {
            if (!opts || typeof opts.format !== 'function') {
                return;
            }
            var ctx = chart.ctx;
            var horizontal = chart.options.indexAxis === 'y';
            var meta = chart.getDatasetMeta(opts.datasetIndex || 0);
            ctx.save();
            ctx.fillStyle = T.ink2;
            ctx.font = '600 12px ' + Chart.defaults.font.family;
            ctx.textAlign = horizontal ? 'left' : 'center';
            ctx.textBaseline = horizontal ? 'middle' : 'bottom';
            meta.data.forEach(function (bar, i) {
                var value = chart.data.datasets[opts.datasetIndex || 0].data[i];
                if (value === null || value === undefined) {
                    return;
                }
                var text = opts.format(value);
                if (horizontal) {
                    ctx.fillText(text, bar.x + 6, bar.y);
                } else {
                    ctx.fillText(text, bar.x, bar.y - 4);
                }
            });
            ctx.restore();
        }
    });

    var number = new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 1});

    window.HRM.charts = {
        tokens: T,

        /** 761 930 033 → "761,9 tr"; 1 250 000 000 → "1,3 tỷ" */
        moneyCompact: function (v) {
            v = Number(v);
            if (Math.abs(v) >= 1e9) {
                return number.format(v / 1e9) + ' tỷ';
            }
            if (Math.abs(v) >= 1e6) {
                return number.format(v / 1e6) + ' tr';
            }
            return number.format(v);
        },

        /** Common cartesian axes: hairline grid on the value axis only. */
        axes: function (horizontal, extra) {
            var value = $.extend(true, {
                beginAtZero: true,
                grid: {color: T.grid, drawTicks: false},
                border: {display: false},
                ticks: {color: T.muted, padding: 6, precision: 0}
            }, extra || {});
            var category = {grid: {display: false}, border: {color: T.axis}, ticks: {color: T.ink2}};
            return horizontal ? {x: value, y: category} : {x: category, y: value};
        },

        /** Table view twin of a chart (WCAG equivalent; values set with .text()). */
        tableView: function ($container, headers, rows) {
            var $table = $('<table class="table table-sm mb-0 mt-2">');
            var $tr = $('<tr>');
            $.each(headers, function (i, h) {
                $tr.append($('<th>').toggleClass('text-end', i > 0).text(h));
            });
            $table.append($('<thead>').append($tr));
            var $body = $('<tbody>');
            $.each(rows, function (_, row) {
                var $r = $('<tr>');
                $.each(row, function (i, cell) {
                    $r.append($('<td>').toggleClass('text-end', i > 0).text(cell));
                });
                $body.append($r);
            });
            $table.append($body);
            $container.empty().append($('<details class="small mt-2">')
                .append($('<summary class="text-body-secondary">').text('Xem bảng số liệu'))
                .append($('<div class="table-responsive">').append($table)));
        },

        /**
         * Tall segmented bar (one whole split into ≤ 6 parts): 28px, 2px surface gaps, share printed
         * inside a block only when it fits, legend of names below.
         */
        segmentedBar: function ($el, parts) {
            var total = parts.reduce(function (s, p) { return s + p.value; }, 0);
            var $bar = $('<div class="seg-bar" role="img">');
            var $legend = $('<div class="seg-legend">');
            var description = [];
            $.each(parts, function (i, p) {
                if (!p.value) {
                    return;
                }
                var share = total ? Math.round(100 * p.value / total) : 0;
                description.push(p.label + ' ' + p.value + ' (' + share + '%)');
                var $seg = $('<div class="seg">').css({flexGrow: p.value, background: T.series[i]})
                    .attr('title', p.label + ': ' + p.value + ' (' + share + '%)')
                    .append($('<span>').text(share + '%'));
                $bar.append($seg);
                $legend.append($('<span class="seg-key">').append($('<i>').css('background', T.series[i])).append(document.createTextNode(p.label)));
            });
            $bar.attr('aria-label', description.join(', '));
            $el.empty().append($bar, $legend);
            $bar.find('.seg').each(function () {      // measure: drop labels that would not fit
                var $s = $(this);
                if ($s.find('span')[0].scrollWidth + 12 > $s.width()) {
                    $s.find('span').remove();
                }
            });
        }
    };
})(window, jQuery);
