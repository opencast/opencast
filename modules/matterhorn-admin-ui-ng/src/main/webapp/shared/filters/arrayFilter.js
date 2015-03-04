angular.module('adminNg.filters')
.filter('joinBy', [function () {
    return function (input, delimiter) {
        return (input || []).join(delimiter || ',');
    };
}]);
