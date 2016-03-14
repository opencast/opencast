/*
 Transforms numbers of bytes into human readable format
 */
angular.module('adminNg.filters')
    .filter('humanBytes', [function () {
        return function (bytesValue) {

            if (angular.isUndefined(bytesValue)) {
                return;
            }

            // best effort, independent on type
            var bytes = parseInt(bytesValue);

            if(isNaN(bytes)) {
                return bytesValue;
            }

            // from http://stackoverflow.com/a/14919494
            var thresh = 1000;
            if (Math.abs(bytes) < thresh) {
                return bytes + ' B';
            }
            var units = ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
            var u = -1;
            do {
                bytes /= thresh;
                ++u;
            } while (Math.abs(bytes) >= thresh && u < units.length - 1);

            return bytes.toFixed(1) + ' ' + units[u];

        };
    }]);
