angular.module('adminNg.services')
.factory('NewSeriesSummary', [function () {
    var Summary = function () {
        this.ud = {};
        this.isValid = function () {
            return true;
        };
        this.isDisabled = false;
    };
    return new Summary();
}]);
