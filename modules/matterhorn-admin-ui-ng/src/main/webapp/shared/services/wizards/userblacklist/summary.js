angular.module('adminNg.services')
.factory('NewUserblacklistSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);
