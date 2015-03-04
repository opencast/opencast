angular.module('adminNg.services')
.factory('BulkMessageSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);
