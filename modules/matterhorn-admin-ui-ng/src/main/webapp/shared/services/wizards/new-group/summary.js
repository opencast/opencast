angular.module('adminNg.services')
.factory('NewGroupSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);
