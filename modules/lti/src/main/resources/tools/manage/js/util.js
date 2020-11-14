// ---------------------------------
//  Custom Checkbox 
// ---------------------------------
// Simple plugin to manage checkboxes - fontawesome
// ------------------------

;
(function($, window, document, undefined) {

    var pluginName = 'customCheckbox';

    function Plugin(element, options) {
        this.element = element;
        this._name = pluginName;
        this._defaults = $.fn.customCheckbox.defaults;
        this.options = $.extend({}, this._defaults, options);

        this.init();
    }

    $.extend(Plugin.prototype, {

        // Initialization logic
        init: function() {
            this.$element = $(this.element);

            var _ref = (this.$element.attr('data-ref') === undefined ? this.$element.attr('id') : this.$element.attr('data-ref'));

            this.ref = (_ref === undefined ? '0' : $(_ref.split(/-|_|\./)).last()[0]);
            this.link = this.$element.is('a') ? this.$element : null;
            this.is_checkbox = this.$element.is("input[type='checkbox']");

            if (this.link === null) {
                this.$element.hide();
                this.link = $('<a id=chkbox-' + this.ref + '></a>');
                this.link.data(pluginName, this);
                this.$element.before(this.link);
            }

            this.link.attr('href', '#')
                .addClass('checkbox')
                .on('click', this._onClick)
                .html('<span class = "fa fa-square-o"></span><span class = "fa fa-check-square-o"></span><span class = "fa fa-minus-square-o"></span>');

            //this.bindEvents();
        },

        // Remove plugin instance completely
        destroy: function() {
            this.unbindEvents();
            this.$element.removeData();
        },

        // Bind events that trigger methods
        bindEvents: function() {
            var plugin = this;

            plugin.$element.on('click' + '.' + plugin._name, function() {
                plugin.someOtherFunction.call(plugin);
            });
        },

        // Unbind events that trigger methods
        unbindEvents: function() {
            this.$element.off('.' + this._name);
        },

        _onClick: function(event) {
            event.preventDefault();

            var plugin = $(this).data(pluginName);

            if (plugin.link.hasClass('all')) {
                plugin.selectNone();

            } else if (plugin.link.hasClass('partial')) {
                plugin.selectAll();

            } else {
                plugin.selectAll();
            }
        },

        selectAll: function(single) {
            var plugin = this,
                single = single == undefined ? true : single;

            plugin.link.removeClass('partial');
            plugin.link.addClass('all');

            if (plugin.is_checkbox)
                plugin.$element.prop('checked', true);

            if (single)
                plugin.$element.trigger('selected.all', plugin.ref);
        },

        selectNone: function(single) {
            var plugin = this,
                single = single == undefined ? true : single;

            plugin.link.removeClass('all partial');

            if (plugin.is_checkbox)
                plugin.$element.prop('checked', false);

            if (single)
                plugin.$element.trigger('selected.none', plugin.ref);
        },

        selectPartial: function(single) {
            var plugin = this,
                single = single == undefined ? true : single;

            if (plugin.options.partial) {
                plugin.link.removeClass('all');
                plugin.link.addClass('partial');

                if (single)
                    plugin.$element.trigger('selected.partial', plugin.ref);
            } else {
                plugin.selectAll();
            }
        },

        callback: function() {
            // Cache onComplete option
            var onComplete = this.options.onComplete;

            if (typeof onComplete === 'function') {
                onComplete.call(this.element);
            }
        }

    });

    $.fn.customCheckbox = function(options) {
        this.each(function() {
            if (!$.data(this, pluginName)) {
                $.data(this, pluginName, new Plugin(this, options));
            }
        });
        return this;
    };

    $.fn.customCheckbox.defaults = {
        ref: 0, // default reference for this item
        partial: false // true to include partial select
    };

    $.fn.singleDatePicker = function(cb) {
        $(this).on('apply.daterangepicker', function(e, picker) {
            cb(picker.startDate, picker);
        });
        return $(this).daterangepicker({
            singleDatePicker: true,
             autoUpdateInput: false
        });
    };

    $.fn.extend({
      timerangepicker: function(opts, cb, cancel) {
        var picker = $('<div/>', {
                       class: 'timepicker range'
                     });

        if (opts && opts.opens) {
          opts.opens.split(' ').forEach(function(open) {
            picker.addClass(open);
          });
        }

        var trigger;
        var setDuration = false;

        if ($(this).attr('name') === 'duration') {
          trigger = $(this).parents('.dateTimeSelector');
          setDuration = true;
        }
        else {
          trigger = $(this);
        }

        trigger.append(picker);
        trigger.addClass('timetrigger');

        picker.append($('<h2/>', {text: 'Start time range'}));

        var timeSelector = $('<div/>', {
                             class: 'timeWrapper pristine'
                           });

        var timeDisplay = $('<div/>', {
                            class: 'timeDisplaySelected'
                           });

        function selectTimer(name, num) {
          var _select = $('<select/>');

          if (typeof name == 'string') {
            _select.attr('name', name);
          }

          _select.append($('<option/>', {value: '', text: (name == 'hour' ? 'HH' : 'MM')}));

          for (var i = 0; i < num; i++) {
            var _option = $('<option/>', {
                            value: (i < 10 ? '0' + i : i),
                             text: (i < 10 ? '0' + i : i)
                          });
            _select.append(_option);
          }

          return _select;
        }

        var displayHour = $('<span/>', {'data-timetype': 'Hour'}).append(selectTimer('hour', 24));
        var displayMinute = $('<span/>', {'data-timetype': 'Minute'}).append(selectTimer('minute', 60));

        timeDisplay
          .append(displayHour)
          .append(displayMinute);

        timeSelector.append(timeDisplay);

        var startSelector = timeSelector.clone(true);
        var endSelector = timeSelector.clone(true);

        picker
          .append(startSelector)
          .append(endSelector);

        var applyBtn = $('<button/>', {
                         class: 'btn btn-success',
                         type: 'button',
                         text: 'Apply'
                       });

        var cancelBtn = $('<button/>', {
                         class: 'btn cancel-btn btn-default',
                         type: 'button',
                         text: 'Cancel'
                       });

        var controls = $('<div/>', {
                         class: 'timepickercontrols'
                       });

        controls
          .append(cancelBtn)
          .append(applyBtn)
          .appendTo(picker);

        applyBtn.on('click', function() {
          var times = {};

          var times = [[],[]];
          picker.find('.timeWrapper').each(function(i) {
            $(this).find('select').each(function() {
              times[i].push($(this).val());
            });
          });

          var allow = true;
          times.forEach(function(timeType, i) {
            if (timeType.reduce(function(collect, cur) { return collect + (cur ? 1 : 0); }, 0) === 1) {
              allow = false;
              picker.find('.timeWrapper:nth-of-type(2n+' + (i+1) + ') select').each(function() {
                $(this).css({
                  borderColor: 'red',
                  boxShadow: '0 0 3px red'
                });
                var self = this;
                setTimeout(function() {
                  self.style.borderColor = '';
                  self.style.boxShadow = '';
                }, 5000);
              });
            }
          });
          /*  
            return;*/
          if (!allow) {
            return;
          }
          if (cb && typeof cb == 'function') {
            cb(times[0].join('') ? times[0].join(':') : null,
               times[1].join('') ?  times[1].join(':') : null,
                $(this).parents('.timetrigger')[0]);
          }
          cancelBtn.trigger('click');
        });

        cancelBtn.on('click', function() {
          picker.find('.timeWrapper').each(function() {
            picker.find('select').each(function() {
              $(this).val('');
            });
          });
          picker.removeClass('active');
        });

      }
    });

    $.fn.extend({
      timesetter: function(opts, cb, cancel) {
        var picker = $('<div/>', {
                       class: 'timepicker setter'
                     });

        if (opts && opts.opens) {
          opts.opens.split(' ').forEach(function(open) {
            picker.addClass(open);
          });
        }

        var trigger;
        var setDuration = false;

        if ($(this).attr('name') === 'duration') {
          trigger = $(this).parents('.dateTimeSelector');
          setDuration = true;
        }
        else {
          trigger = $(this);
        }

        trigger.append(picker);
        trigger.addClass('timetrigger');

        var timeSelector = $('<div/>', {
                             class: 'timeWrapper pristine'
                           });

        var timeDisplay = $('<div/>', {
                            class: 'timeDisplaySelected'
                           });

        var setTime = [-1, -1];

        if (trigger.data('setTime')) {
          setTime = trigger.data('setTime').split(':').filter(function(unit, i) { return i < 2 }).map(function(unit) { return +unit });
        }
        else if (opts && opts.setTime) {
	  setTime = opts.setTime.split(':').filter(function(unit, i) { return i < 2 }).map(function(unit) { return +unit });
          trigger.data('setTime', opts.setTime);
        }

        function selectTimer(name, num) {
          var _select = $('<select/>');

          if (typeof name == 'string') {
            _select.attr('name', name);
          }

          _select.append($('<option/>', {value: '', text: (name == 'hour' ? 'HH' : 'MM')}));

          for (var i = 0; i < num; i++) {
            var _option = $('<option/>', {
                            value: (i < 10 ? '0' + i : i),
                             text: (i < 10 ? '0' + i : i)
                          });
            _select.append(_option);
          }

          if (name == 'hour' && setTime[0] != -1) {
              _select.val((setTime[0] < 10 ? '0' : '') + setTime[0]);
          }
          else if (setTime[1] != -1) {
              _select.val((setTime[1] < 10 ? '0' : '') + setTime[1]);
          }
          

          return _select;
        }

        var displayHour = $('<span/>', {'data-timetype': 'Hour'}).append(selectTimer('hour', 24));
        var displayMinute = $('<span/>', {'data-timetype': 'Minute'}).append(selectTimer('minute', 60));

        timeDisplay
          .append(displayHour)
          .append(displayMinute);

        timeSelector.append(timeDisplay);

        picker.append(timeSelector);

        var durationSelector = $('<div/>', {
                                 class: 'timeWrapper pristine',
                                 'data-title': 'Duration'
                               });

        var durationDisplay = $('<div/>', {
                                class: 'timeDisplaySelected'
                               });

        var input = $('<input/>', {name: 'duration_range', type: 'range', max: 180, min: 5, step: 5, value: opts.setDuration || 55});
        var inputDisplay = $('<p/>', {class: 'durationDisplay'});

        durationDisplay.append(input);
        durationDisplay.append(inputDisplay.css('width', '100%'));
        durationSelector.append(durationDisplay);
        picker.append(durationSelector);

        input.trigger('change');

        var applyBtn = $('<button/>', {
                         class: 'btn btn-success',
                         type: 'button',
                         text: 'Apply'
                       });

        var cancelBtn = $('<button/>', {
                         class: 'btn cancel-btn btn-default',
                         type: 'button',
                         text: 'Cancel'
                       });

        var controls = $('<div/>', {
                         class: 'timepickercontrols'
                       });

        controls
          .append(applyBtn)
          .append(cancelBtn)
          .appendTo(picker);

        applyBtn.on('click', function() {
          var times = {};

          var timeOK = true;

          var startTime = [];
          picker.find('select').each(function() {
            if (!$(this).val()) {
              timeOK = false;
            }
            startTime.push($(this).val());
          });

          if (!timeOK) {
            picker.find('select').each(function() {
              $(this).css({
                borderColor: 'red',
                boxShadow: '0 0 3px red'
              });
              var self = this;
              setTimeout(function() {
                self.style.borderColor = '';
                self.style.boxShadow = '';
              }, 5000);
            });
            return;
          }
          var duration = picker.find('input[type=range]')[0].value;

          var end = parseInt(startTime[0])*60 + parseInt(startTime[1]) + parseInt(duration);
          var endHours = parseInt(end/60);
          var endTime = [(endHours < 10 ? '0' : '') + endHours, end%60 + ''];
          if (cb && typeof cb == 'function') {
            cb(startTime.join(':') || null, endTime.join(':') || null, $(this).parents('.timetrigger')[0]);
          }
          cancelBtn.trigger('click');
        });

        cancelBtn.on('click', function() {
          picker.find('.timeWrapper').each(function() {
            $(this).addClass('pristine');
            input.trigger('change');
          });
          picker.removeClass('active');
        });
      }
    });


})(jQuery, window, document);
