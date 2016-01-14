angular.module('adminNg.filters')
.filter('presentValue', [function () {
    return function (input, delimiter) {
        if (angular.isUndefined(delimiter)) {
            delimiter = ', ';
        }
        if (input instanceof Array) {
            return input.join(delimiter);
        } else {
            return input;
        }
    };
}]);
