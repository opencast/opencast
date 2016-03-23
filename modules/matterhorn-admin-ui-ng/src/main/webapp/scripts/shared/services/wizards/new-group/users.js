angular.module('adminNg.services')
.factory('NewGroupUsers', ['ResourcesListResource', '$location', function (ResourcesListResource) {
    var Users = function () {
        var me = this;
        me.users = {
            available: ResourcesListResource.query({ resource: 'USERS.INVERSE'}),
            selected:  [],
            i18n: 'USERS.GROUPS.DETAILS.USERS',
            searchable: true
        };

        this.reset = function () {
        };
        this.reset();

        this.isValid = function () {
            return true;
        };

        this.getUsersList = function () {
            var list = '';
            
            angular.forEach(me.users.selected, function (user, index) {
                list += user.name + ((index + 1) === me.users.selected.length ? '' : ', ');
            });

            return list;
        };
    };
    return new Users();
}]);
