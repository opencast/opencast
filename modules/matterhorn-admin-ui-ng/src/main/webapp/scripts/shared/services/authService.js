angular.module('adminNg.services')
.factory('AuthService', ['IdentityResource', function (IdentityResource) {
    var AuthService = function () {
        var me = this,
            isAdmin = false,
            isUserLoaded = false,
            callbacks = [],
            identity,
            userRole,
            isAuthorizedAs = function (role) {
                if (angular.isUndefined(me.user.roles)) {
                    return false;
                }
                return isAdmin ||
                    (angular.isArray(me.user.roles) && me.user.roles.indexOf(role) > -1) ||
                    me.user.roles === role;
            };

        this.user = {};

        this.loadUser = function () {
            identity = IdentityResource.get();
            identity.$promise.then(function (user) {
                // Users holding the global admin role shall always be authorized to do anything
                var globalAdminRole = "ROLE_ADMIN";
                me.user = user;
                isAdmin = angular.isDefined(globalAdminRole) && user.roles.indexOf(globalAdminRole) > -1;
                if (angular.isDefined(user.userRole)) {
                    userRole = user.userRole;
                }
                isUserLoaded = true;
                angular.forEach(callbacks, function (item) {
                    isAuthorizedAs(item.role) ? item.success() : item.error();
                });
            });
        };

        this.getUser = function () {
            return identity;
        };

        this.getUserRole = function () {
            return userRole;
        };

        this.userIsAuthorizedAs = function (role, success, error) {
            if (angular.isUndefined(success)) {
                return isAuthorizedAs(role);
            }

            if (isUserLoaded) {
                isAuthorizedAs(role) ? success() : error();
            } else {
                callbacks.push({
                    role    : role,
                    success : success,
                    error   : error
                });
            }
        };

        this.loadUser();
    };

    return new AuthService();
}]);
