angular.module('adminNg.filters')
.filter('localizeDate', ['Language', function (Language) {
    return function (input, type, format) {
        if (angular.isUndefined(format)) {
            format = 'short';
        }

        switch (type) {
            case 'date':
                return Language.formatDate(format, input);
            case 'dateTime':
                return Language.formatDateTime(format, input);
            case 'time':
                if (!angular.isDate(Date.parse(input))) {
                    return input;
                } 

                return Language.formatTime(format, input);
            default:
                return input;
        }
    };
}]);
