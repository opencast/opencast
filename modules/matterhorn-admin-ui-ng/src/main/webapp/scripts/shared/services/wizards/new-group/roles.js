angular.module('adminNg.services')
.factory('NewGroupRoles', ['UsersResource', 'UserRolesResource', 'ResourcesListResource', function (UsersResource, UserRolesResource, ResourcesListResource) {
    var Roles = function () {
        var me = this;

        this.reset = function () {
            me.roles = {
                available: UserRolesResource.query({limit: 0, offset: 0, filter: 'role_target:USER'}),
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
