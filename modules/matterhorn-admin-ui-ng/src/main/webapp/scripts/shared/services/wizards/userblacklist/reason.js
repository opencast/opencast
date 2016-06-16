angular.module('adminNg.services')
.factory('NewUserblacklistReason', ['ResourcesListResource',
function (ResourcesListResource) {
    var Reason = function () {
        var me = this;

        this.reset = function () {
            me.ud = {};
        };
        this.reset();

        // Hard coded reasons, as requested.
        this.reasons = ResourcesListResource.get({ resource: 'BLACKLISTS.USERS.REASONS' });

        this.isValid = function () {
            return (me.ud.reason ? true:false);
        };
    };
    return new Reason();
}]);
