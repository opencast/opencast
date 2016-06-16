angular.module('adminNg.services')
.factory('EmailtemplateSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);
