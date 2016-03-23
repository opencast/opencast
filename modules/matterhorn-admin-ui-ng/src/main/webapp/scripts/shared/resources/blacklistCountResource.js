angular.module('adminNg.resources')
.factory('BlacklistCountResource', ['$resource', function ($resource) {
    return $resource('/blacklist/blacklistCount', {}, {
        save: {
            method: 'GET',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
    });
}]);
