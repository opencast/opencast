(function ($) {
    $.widget("ui.timefield", {
        // set default values
        options: {
            value: 0
        },

        /**
         * create the timefield
         */
        _create: function () {
            var self = this;
            this.inputItem = $('<input type="text" />');
            this.inputItem.val(this._format(this.options.value));
            this.inputItem.focusout(function (e) {
                var val = self.inputItem.val();
                val = val.split(':');
                if (val.length == 3) {
                    var newVal = parseInt(val[0]) * 3600;
                    newVal += parseInt(val[1]) * 60;
                    newVal += parseFloat(val[2]);
                    self._setOption('value', newVal);
                }
            });
            this.inputItem.keyup(function (evt) {
                if (evt.keyCode == 13) {
                    var val = self.inputItem.val();
                    val = val.split(':');
                    if (val.length == 3) {
                        var newVal = parseInt(val[0]) * 3600;
                        newVal += parseInt(val[1]) * 60;
                        newVal += parseFloat(val[2]);
                        self._setOption('value', newVal);
                    }
                }
            });
            this.element.append(this.inputItem);
        },

        /**
         * format the number of seconds and milliseconds to something like
         * hh:MM:ss.mmmm
         */
        _format: function (seconds) {
            if (typeof seconds == "string") {
                seconds = parseFloat(seconds);
            }

            var h = "00";
            var m = "00";
            var s = "00";
            var ms = "00";
            if (!isNaN(seconds) && (seconds >= 0)) {
                var tmpH = Math.floor(seconds / 3600);
                var tmpM = Math.floor((seconds - (tmpH * 3600)) / 60);
                var tmpS = Math.floor(seconds - (tmpH * 3600) - (tmpM * 60));
                var tmpMS = seconds - tmpS;
                h = (tmpH < 10) ? "0" + tmpH : (Math.floor(seconds / 3600) + "");
                m = (tmpM < 10) ? "0" + tmpM : (tmpM + "");
                s = (tmpS < 10) ? "0" + tmpS : (tmpS + "");
                ms = tmpMS + "";
                var indexOfSDot = ms.indexOf(".");
                if (indexOfSDot != -1) {
                    ms = ms.substr(indexOfSDot + 1, ms.length);
                }
                ms = ms.substr(0, 4);
                while (ms.length < 4) {
                    ms += "0";
                }
            }
            return h + ":" + m + ":" + s + "." + ms;
        },

        /**
         * set an option
         */
        _setOption: function (key, value) {
            switch (key) {

            case "value":
                this.inputItem.val(this._format(value));
                this.options.value = value;
                break;
            default:
                this.options[key] = value;
            }
        },

    });
})(jQuery);
