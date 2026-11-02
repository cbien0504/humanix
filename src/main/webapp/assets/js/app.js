/*!
 * HRM front-end core (jQuery 3).
 * Rules followed by every page script:
 *   - no inline JavaScript in JSPs (the Content-Security-Policy forbids it);
 *   - untrusted data is inserted with .text() / jQuery element builders, never as HTML strings;
 *   - every AJAX call carries the CSRF token (X-CSRF-Token header).
 */
(function (window, $) {
    'use strict';

    var ctx = $('meta[name="context-path"]').attr('content') || '';
    var csrfToken = $('meta[name="csrf-token"]').attr('content') || '';

    $.ajaxSetup({
        headers: {'X-CSRF-Token': csrfToken, 'Accept': 'application/json'},
        cache: false
    });

    var HRM = {ctx: ctx, csrfToken: csrfToken};

    HRM.url = function (path) {
        return ctx + path;
    };

    /** HTML-escapes a value (only for the rare places that must build markup strings). */
    HRM.esc = function (value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value).replace(/[&<>"']/g, function (c) {
            return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c];
        });
    };

    HRM.qs = function (name) {
        return new URLSearchParams(window.location.search).get(name);
    };

    // ------------------------------------------------------------------ formatting
    var moneyFormat = new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 0});
    HRM.fmt = {
        money: function (v) {
            return v === null || v === undefined || v === '' ? '' : moneyFormat.format(Math.round(Number(v)));
        },
        number: function (v, digits) {
            if (v === null || v === undefined || v === '') {
                return '';
            }
            return new Intl.NumberFormat('vi-VN', {maximumFractionDigits: digits === undefined ? 1 : digits}).format(Number(v));
        },
        /** "2026-11-02" or "2026-11-02T08:15:00" → "02/11/2026" */
        date: function (iso) {
            if (!iso) {
                return '';
            }
            var p = String(iso).substring(0, 10).split('-');
            return p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : String(iso);
        },
        /** local date-time "2026-11-02T08:15:00" → "08:15" */
        time: function (iso) {
            if (!iso) {
                return '';
            }
            var t = String(iso).split('T')[1] || '';
            return t.substring(0, 5);
        },
        /** local date-time → "02/11/2026 08:15" */
        localDateTime: function (iso) {
            return iso ? HRM.fmt.date(iso) + ' ' + HRM.fmt.time(iso) : '';
        },
        /** UTC instant "2026-11-02T01:15:00Z" → browser local "02/11/2026, 08:15" */
        instant: function (iso) {
            if (!iso) {
                return '';
            }
            var d = new Date(iso);
            return isNaN(d.getTime()) ? String(iso) : d.toLocaleString('vi-VN', {dateStyle: 'short', timeStyle: 'short'});
        }
    };

    // ------------------------------------------------------------------ labels & badges
    HRM.labels = {
        employeeStatus: {PROBATION: ['Thử việc', 'warning'], ACTIVE: ['Chính thức', 'success'], ON_LEAVE: ['Nghỉ dài hạn', 'info'], TERMINATED: ['Đã nghỉ việc', 'secondary']},
        attendanceStatus: {PRESENT: ['Đúng giờ', 'success'], LATE: ['Đi muộn', 'warning'], HALF_DAY: ['Nửa ngày', 'info'], ABSENT: ['Vắng', 'danger']},
        leaveStatus: {PENDING: ['Chờ duyệt', 'warning'], APPROVED: ['Đã duyệt', 'success'], REJECTED: ['Từ chối', 'danger'], CANCELLED: ['Đã hủy', 'secondary']},
        payrollStatus: {OPEN: ['Mở', 'secondary'], CALCULATED: ['Đã tính', 'info'], APPROVED: ['Đã duyệt', 'primary'], PAID: ['Đã chi trả', 'success']},
        jobStatus: {DRAFT: ['Nháp', 'secondary'], OPEN: ['Đang tuyển', 'success'], ON_HOLD: ['Tạm dừng', 'warning'], CLOSED: ['Đã đóng', 'dark']},
        candidateStage: {APPLIED: ['Mới nộp', 'secondary'], SCREENING: ['Sàng lọc', 'info'], INTERVIEW: ['Phỏng vấn', 'primary'], OFFER: ['Đề nghị', 'warning'], HIRED: ['Đã tuyển', 'success'], REJECTED: ['Từ chối', 'danger'], WITHDRAWN: ['Rút hồ sơ', 'dark']},
        cycleStatus: {DRAFT: ['Nháp', 'secondary'], OPEN: ['Đang mở', 'success'], CLOSED: ['Đã đóng', 'dark']},
        reviewStatus: {PENDING_SELF: ['Chờ tự đánh giá', 'warning'], PENDING_MANAGER: ['Chờ quản lý chấm', 'info'], COMPLETED: ['Hoàn thành', 'success']},
        validation: {PASS: ['PASS', 'success'], FAIL: ['FAIL', 'danger'], WARN: ['WARN', 'warning']},
        gender: {M: 'Nam', F: 'Nữ', O: 'Khác'}
    };

    HRM.label = function (group, value) {
        var def = (HRM.labels[group] || {})[value];
        if (!def) {
            return value || '';
        }
        return Array.isArray(def) ? def[0] : def;
    };

    HRM.badge = function (group, value) {
        var def = (HRM.labels[group] || {})[value];
        if (!def || !Array.isArray(def)) {
            return $('<span class="badge text-bg-light">').text(value || '');
        }
        return $('<span class="badge">').addClass('text-bg-' + def[1]).text(def[0]);
    };

    // ------------------------------------------------------------------ toasts
    HRM.toast = function (message, type) {
        type = type || 'success';
        var icon = {success: 'check-circle-fill', danger: 'exclamation-octagon-fill', warning: 'exclamation-triangle-fill', info: 'info-circle-fill'}[type];
        var $toast = $('<div class="toast align-items-center border-0" role="alert" aria-atomic="true">').addClass('text-bg-' + type);
        var $body = $('<div class="toast-body d-flex gap-2 align-items-start">')
            .append($('<i class="bi">').addClass('bi-' + (icon || 'info-circle-fill')))
            .append($('<span>').text(message));
        var $close = $('<button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Đóng">');
        if (type !== 'warning') {
            $close.addClass('btn-close-white');
        }
        $toast.append($('<div class="d-flex">').append($body, $close));
        $('#toastArea').append($toast);
        $toast.on('hidden.bs.toast', function () {
            $toast.remove();
        });
        bootstrap.Toast.getOrCreateInstance($toast[0], {delay: type === 'danger' ? 7000 : 4000}).show();
    };

    // ------------------------------------------------------------------ AJAX
    HRM.errorMessage = function (xhr) {
        var body = xhr.responseJSON;
        if (body && body.message) {
            return body.message + (body.status >= 500 && body.requestId ? ' (Mã tra cứu: ' + body.requestId + ')' : '');
        }
        if (xhr.status === 0) {
            return 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.';
        }
        return 'Lỗi ' + xhr.status + (xhr.statusText ? ': ' + xhr.statusText : '');
    };

    /**
     * JSON request helper returning a jQuery promise.
     * options.form   – a form whose fields receive server-side fieldErrors
     * options.silent – do not show an error toast
     * A 401 (session expired) redirects to the login page.
     */
    HRM.api = function (method, path, data, options) {
        options = options || {};
        var settings = {url: HRM.url(path), method: method, dataType: 'json'};
        if (data !== undefined && data !== null) {
            if (method === 'GET') {
                settings.data = data;
            } else {
                settings.data = JSON.stringify(data);
                settings.contentType = 'application/json; charset=UTF-8';
            }
        }
        return $.ajax(settings).fail(function (xhr) {
            if (xhr.status === 401) {
                window.location.href = HRM.url('/login?expired=1');
                return;
            }
            var body = xhr.responseJSON || {};
            var hasFieldErrors = body.fieldErrors && Object.keys(body.fieldErrors).length > 0;
            if (options.form) {
                HRM.form.showErrors(options.form, body.fieldErrors, hasFieldErrors ? null : HRM.errorMessage(xhr));
            }
            if (!options.silent && !(options.form && hasFieldErrors)) {
                HRM.toast(HRM.errorMessage(xhr), 'danger');
            }
        });
    };
    HRM.get = function (path, data, options) {
        return HRM.api('GET', path, data, options);
    };
    HRM.post = function (path, data, options) {
        return HRM.api('POST', path, data || {}, options);
    };
    HRM.put = function (path, data, options) {
        return HRM.api('PUT', path, data, options);
    };
    HRM.del = function (path, options) {
        return HRM.api('DELETE', path, null, options);
    };

    // ------------------------------------------------------------------ forms
    var RULES = {
        phone: [/^(0|\+84)\d{9}$/, 'Số điện thoại không hợp lệ (VD: 0912345678).'],
        nationalId: [/^\d{12}$/, 'CCCD gồm đúng 12 chữ số.'],
        code: [/^[A-Z0-9-]{3,20}$/, 'Chỉ gồm chữ in hoa, chữ số, dấu gạch ngang (3–20 ký tự).'],
        password: [/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/, 'Tối thiểu 8 ký tự, có chữ hoa, chữ thường và chữ số.']
    };

    HRM.form = {
        /** Named fields → plain object; blank → null; data-type="number" → Number; checkbox → boolean. */
        toObject: function ($form) {
            var obj = {};
            $form.find('input[name], select[name], textarea[name]').each(function () {
                var $el = $(this);
                if (this.type === 'checkbox') {
                    obj[this.name] = this.checked;
                    return;
                }
                if (this.type === 'radio' && !this.checked) {
                    return;
                }
                var val = $el.val();
                if (!this.multiple) {
                    val = val === null || String(val).trim() === '' ? null : String(val).trim();
                    if (val !== null && $el.data('type') === 'number') {
                        val = Number(val);
                    }
                }
                obj[this.name] = val;
            });
            return obj;
        },

        fill: function ($form, data) {
            $form[0].reset();
            HRM.form.clearErrors($form);
            $.each(data || {}, function (key, value) {
                var $el = $form.find('[name="' + key + '"]');
                if (!$el.length) {
                    return;
                }
                if ($el.is(':checkbox')) {
                    $el.prop('checked', !!value);
                } else if ($el.is(':radio')) {
                    $el.filter('[value="' + value + '"]').prop('checked', true);
                } else {
                    $el.val(value === null || value === undefined ? '' : value);
                }
            });
        },

        clearErrors: function ($form) {
            $form.removeClass('was-validated');
            $form.find('.is-invalid').removeClass('is-invalid');
            $form.find('.invalid-feedback.server').remove();
            $form.find('.form-alert').addClass('d-none').text('');
            $form.find('[data-rule], [data-match], [data-after]').each(function () {
                this.setCustomValidity('');
            });
        },

        /** Shows server-side field errors under their inputs; other errors in .form-alert. */
        showErrors: function ($form, fieldErrors, generalMessage) {
            var unplaced = [];
            $.each(fieldErrors || {}, function (field, message) {
                var $el = $form.find('[name="' + field + '"]');
                if (!$el.length) {
                    unplaced.push(message);
                    return;
                }
                $el.addClass('is-invalid');
                $el.siblings('.invalid-feedback').not('.server').hide();
                $('<div class="invalid-feedback server">').text(message).insertAfter($el.last());
            });
            if (generalMessage) {
                unplaced.unshift(generalMessage);
            }
            if (unplaced.length) {
                $form.find('.form-alert').removeClass('d-none').text(unplaced.join(' '));
            }
            $form.find('.is-invalid').first().trigger('focus');
        },

        /** HTML5 constraint validation + custom rules (data-rule, data-match, data-after). */
        validate: function ($form) {
            HRM.form.clearErrors($form);
            // drop the inline display left by showErrors(); .show() would force EVERY message visible
            $form.find('.invalid-feedback').not('.server').css('display', '');
            $form.find('[data-rule]').each(function () {
                var rule = RULES[$(this).data('rule')];
                var value = $(this).val();
                this.setCustomValidity(rule && value && !rule[0].test(value) ? rule[1] : '');
                if (rule && value && !rule[0].test(value)) {
                    $(this).siblings('.invalid-feedback').text(rule[1]);
                }
            });
            $form.find('[data-match]').each(function () {
                var other = $form.find('[name="' + $(this).data('match') + '"]').val();
                this.setCustomValidity($(this).val() !== other ? 'Không khớp' : '');
            });
            $form.find('[data-after]').each(function () {
                var start = $form.find('[name="' + $(this).data('after') + '"]').val();
                var end = $(this).val();
                this.setCustomValidity(start && end && end < start ? 'Phải sau ngày bắt đầu' : '');
            });
            var ok = $form[0].checkValidity();
            $form.addClass('was-validated');
            if (!ok) {
                $form.find(':invalid').first().trigger('focus');
            }
            return ok;
        }
    };

    // auto upper-case for code inputs
    $(document).on('input', 'input[data-uppercase]', function () {
        var pos = this.selectionStart;
        this.value = this.value.toUpperCase();
        this.setSelectionRange(pos, pos);
    });

    /** Fills a <select> with options built from items (text via .text(), never HTML). */
    HRM.fillSelect = function ($select, items, valueKey, textFn, placeholder) {
        var current = $select.val();
        $select.empty();
        if (placeholder !== undefined) {
            $select.append($('<option value="">').text(placeholder));
        }
        $.each(items || [], function (_, item) {
            $select.append($('<option>').val(item[valueKey]).text(typeof textFn === 'function' ? textFn(item) : item[textFn]));
        });
        if (current) {
            $select.val(current);
        }
    };

    HRM.modal = function (selector) {
        return bootstrap.Modal.getOrCreateInstance($(selector)[0]);
    };

    /**
     * Promise-based confirmation dialog.
     * options: title, okText, variant ('primary'|'danger'…), input (true = ask for a text),
     *          inputRequired, placeholder. Resolves with the entered text (or null).
     */
    HRM.confirm = function (message, options) {
        options = options || {};
        var deferred = $.Deferred();
        var $modal = $('<div class="modal fade" tabindex="-1" aria-hidden="true">' +
            '<div class="modal-dialog modal-dialog-centered"><div class="modal-content">' +
            '<div class="modal-header"><h5 class="modal-title"></h5>' +
            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div>' +
            '<div class="modal-body"><p class="mb-2 confirm-message"></p></div>' +
            '<div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>' +
            '<button type="button" class="btn btn-ok"></button></div></div></div></div>');
        $modal.find('.modal-title').text(options.title || 'Xác nhận');
        $modal.find('.confirm-message').text(message);
        $modal.find('.btn-ok').addClass('btn-' + (options.variant || 'primary')).text(options.okText || 'Đồng ý');
        if (options.input) {
            $('<textarea class="form-control" rows="3" maxlength="500">').attr('placeholder', options.placeholder || '')
                .appendTo($modal.find('.modal-body'));
        }
        var confirmed = false;
        var modal = new bootstrap.Modal($modal[0]);
        $modal.find('.btn-ok').on('click', function () {
            var text = options.input ? $modal.find('textarea').val().trim() : null;
            if (options.input && options.inputRequired && !text) {
                $modal.find('textarea').addClass('is-invalid').trigger('focus');
                return;
            }
            confirmed = true;
            modal.hide();
            deferred.resolve(text || null);
        });
        $modal.on('hidden.bs.modal', function () {
            if (!confirmed) {
                deferred.reject();
            }
            $modal.remove();
        });
        $('body').append($modal);
        modal.show();
        return deferred.promise();
    };

    // ------------------------------------------------------------------ data table
    /**
     * Server-side paged table bound to an endpoint returning
     * {items, page, size, totalItems, totalPages}.
     * options.columns: [{title, key | render(row) → text or jQuery element, sortKey, className}]
     * options.filterForm: form whose fields are sent as query parameters.
     */
    function DataTable($table, options) {
        this.$table = $table;
        this.o = $.extend({size: 10, emptyText: 'Không có dữ liệu.'}, options);
        this.state = {page: 1, sort: options.sort || null, dir: options.dir || 'asc'};
        if (!$table.find('thead').length) {
            $table.prepend('<thead>');
        }
        this.$tbody = $table.find('tbody').length ? $table.find('tbody') : $('<tbody>').appendTo($table);
        this.$footer = options.footer ? $(options.footer) : $table.closest('.card').find('.data-table-footer');
        this.buildHeader();
        this.bind();
    }

    DataTable.prototype.buildHeader = function () {
        var self = this;
        var $tr = $('<tr>');
        $.each(this.o.columns, function (_, col) {
            var $th = $('<th scope="col">').text(col.title || '');
            if (col.className) {
                $th.addClass(col.className);
            }
            if (col.sortKey) {
                $th.addClass('sortable').attr('data-sort', col.sortKey);
                if (self.state.sort === col.sortKey) {
                    $th.addClass('sort-' + self.state.dir);
                }
            }
            $tr.append($th);
        });
        this.$table.find('thead').empty().append($tr);
    };

    DataTable.prototype.bind = function () {
        var self = this;
        this.$table.on('click', 'th.sortable', function () {
            var key = $(this).data('sort');
            self.state.dir = self.state.sort === key && self.state.dir === 'asc' ? 'desc' : 'asc';
            self.state.sort = key;
            self.$table.find('th.sortable').removeClass('sort-asc sort-desc');
            $(this).addClass('sort-' + self.state.dir);
            self.reload(1);
        });
        this.$footer.on('click', '.page-link[data-page]', function (e) {
            e.preventDefault();
            self.reload(Number($(this).data('page')));
        });
        if (this.o.filterForm) {
            var $f = $(this.o.filterForm);
            var timer;
            $f.on('submit', function (e) {
                e.preventDefault();
                self.reload(1);
            });
            $f.on('change', 'select, input[type=date], input[type=month]', function () {
                self.reload(1);
            });
            $f.on('input', 'input[type=search]', function () {
                clearTimeout(timer);
                timer = setTimeout(function () {
                    self.reload(1);
                }, 350);
            });
        }
    };

    DataTable.prototype.params = function () {
        var p = this.o.filterForm ? HRM.form.toObject($(this.o.filterForm)) : {};
        $.each(p, function (k, v) {
            if (v === null) {
                delete p[k];
            }
        });
        p.page = this.state.page;
        p.size = this.o.size;
        if (this.state.sort) {
            p.sort = this.state.sort;
            p.dir = this.state.dir;
        }
        return p;
    };

    DataTable.prototype.reload = function (page) {
        var self = this;
        if (page) {
            this.state.page = page;
        }
        this.$tbody.html('<tr><td colspan="' + this.o.columns.length + '"><div class="spinner-overlay">' +
            '<div class="spinner-border spinner-border-sm text-primary" role="status"></div></div></td></tr>');
        return HRM.get(this.o.url, this.params()).done(function (data) {
            self.render(data);
            if (self.o.onLoad) {
                self.o.onLoad(data);
            }
        }).fail(function () {
            self.$tbody.empty().append($('<tr>').append(
                $('<td class="empty-state">').attr('colspan', self.o.columns.length).text('Không tải được dữ liệu.')));
        });
    };

    DataTable.prototype.render = function (data) {
        var self = this;
        this.lastData = data;
        this.$tbody.empty();
        if (!data.items || !data.items.length) {
            this.$tbody.append($('<tr>').append(
                $('<td class="empty-state">').attr('colspan', this.o.columns.length).text(this.o.emptyText)));
        }
        $.each(data.items || [], function (_, row) {
            var $tr = $('<tr>').data('row', row);
            $.each(self.o.columns, function (_, col) {
                var $td = $('<td>');
                if (col.className) {
                    $td.addClass(col.className);
                }
                var value = col.render ? col.render(row, $tr) : row[col.key];
                if (value instanceof $ || (value && value.nodeType)) {
                    $td.append(value);
                } else {
                    $td.text(value === null || value === undefined ? '' : value);
                }
                $tr.append($td);
            });
            self.$tbody.append($tr);
        });
        this.renderFooter(data);
    };

    DataTable.prototype.renderFooter = function (data) {
        var from = data.totalItems === 0 ? 0 : (data.page - 1) * data.size + 1;
        var to = Math.min(data.page * data.size, data.totalItems);
        var $info = $('<div class="small text-body-secondary">')
            .text('Hiển thị ' + from + '–' + to + ' / ' + HRM.fmt.number(data.totalItems, 0) + ' bản ghi');
        var $ul = $('<ul class="pagination pagination-sm mb-0">');
        function item(label, page, disabled, active) {
            var $li = $('<li class="page-item">').toggleClass('disabled', !!disabled).toggleClass('active', !!active);
            $li.append($('<a class="page-link" href="#">').attr('data-page', page).text(label));
            return $li;
        }
        var total = Math.max(data.totalPages, 1);
        $ul.append(item('‹', data.page - 1, data.page <= 1));
        var start = Math.max(1, data.page - 2);
        var end = Math.min(total, start + 4);
        start = Math.max(1, end - 4);
        for (var i = start; i <= end; i++) {
            $ul.append(item(String(i), i, false, i === data.page));
        }
        $ul.append(item('›', data.page + 1, data.page >= total));
        this.$footer.empty().append($info, $('<nav aria-label="Phân trang">').append($ul));
    };

    HRM.DataTable = DataTable;

    /** Small debounce helper. */
    HRM.debounce = function (fn, ms) {
        var t;
        return function () {
            var args = arguments;
            var self = this;
            clearTimeout(t);
            t = setTimeout(function () {
                fn.apply(self, args);
            }, ms);
        };
    };

    window.HRM = HRM;
})(window, jQuery);
