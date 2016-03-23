angular.module('adminNg.filters')
    .filter('humanDuration', [function () {
        return function (durationInMsecValue) {

            if(angular.isUndefined(durationInMsecValue)) {
                return;
            }

            // best effort, independent on type
            var durationInMsec = parseInt(durationInMsecValue);

            if(isNaN(durationInMsec)) {
                return durationInMsecValue;
            }

            var durationInSec = parseInt(durationInMsec / 1000);

            var min = 60;
            var hour = min * min;

            var hours = parseInt(durationInSec / hour);
            var rest = durationInSec % hour;
            var minutes = parseInt(rest / min);
            rest = rest % min;
            var seconds =  parseInt(rest % min);

            if(seconds < 10) {
                // add leading zero
                seconds = '0' + seconds;
            }

            if (hours > 0 && minutes < 10) {
                minutes = '0' + minutes;
            }

            var minWithSec = minutes + ':' + seconds;
            if (hours > 0) {
                return hours + ':' + minWithSec;
            }
            return minWithSec;
        };
    }]);