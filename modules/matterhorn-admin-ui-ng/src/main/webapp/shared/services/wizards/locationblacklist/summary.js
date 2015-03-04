angular.module('adminNg.services')
.factory('NewLocationblacklistSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);
