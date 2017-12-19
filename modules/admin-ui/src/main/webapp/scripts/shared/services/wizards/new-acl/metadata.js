angular.module('adminNg.services')
.factory('NewAclMetadata', [function () {
    var Metadata = function () {
        var me = this;

        this.reset = function () {
            me.metadata = {
                name: ''
            };
        };
        this.reset();

        this.isValid = function () {
            return angular.isDefined(me.metadata) && (me.metadata.name.length > 0);
        };
    };
    return new Metadata();
}]);
