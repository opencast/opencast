angular.module('adminNg.services')
.factory('AuthService', ['IdentityResource', function (IdentityResource) {
    var AuthService = function () {
        var me = this,
            isAdmin = false,
            isUserLoaded = false,
            callbacks = [],
            identity,
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
                var adminRole = user.org.adminRole;
                me.user = user;
                isAdmin = angular.isDefined(adminRole) && user.roles.indexOf(adminRole) > -1;
                isUserLoaded = true;
                angular.forEach(callbacks, function (item) {
                    isAuthorizedAs(item.role) ? item.success() : item.error();
                });
            });
        };

        this.getUser = function () {
            return identity;
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
