angular.module('adminNg.services')
.factory('NewSeriesSummary', [function () {
    var Summary = function () {
        this.ud = {};
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);
