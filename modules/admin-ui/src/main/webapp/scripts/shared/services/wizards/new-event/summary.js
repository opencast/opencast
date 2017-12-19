angular.module('adminNg.services')
.factory('NewEventSummary', [function () {
    var Summary = function () {
        var me = this;
        me.ud = {};
        this.isValid = function () {
            return true;
        };

        this.reset = function () {
            me.ud = {};
        };
    };
    return new Summary();
}]);
