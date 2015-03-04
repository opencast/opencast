angular.module('adminNg.services')
.factory('NewGroupRoles', ['UsersResource', 'ResourcesListResource', function (UsersResource, ResourcesListResource) {
    var Roles = function () {
        var me = this;

        this.reset = function () {
            me.roles = {
                available: ResourcesListResource.query({ resource: 'ROLES'}),
                selected:  [],
                i18n: 'USERS.GROUPS.DETAILS.ROLES',
                searchable: true
            };
        };
        this.reset();

        this.isValid = function () {
            return true;
        };

        this.getRolesList = function () {
            var list = '';

            angular.forEach(me.roles.selected, function (role, index) {
                list += role.name + ((index + 1) === me.roles.selected.length ? '' : ', ');
            });

            return list;
        };

    };
    return new Roles();
}]);
