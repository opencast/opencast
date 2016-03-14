angular.module('adminNg.filters')
.filter('removeQueryParams', [function () {
    return function (input) {
        if (angular.isUndefined(input)) {
           return input;
        }
        return input.split('?')[0];
    };
}]);
