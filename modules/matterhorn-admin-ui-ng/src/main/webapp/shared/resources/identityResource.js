angular.module('adminNg.resources')
.factory('IdentityResource', ['$resource', function ($resource) {
    return $resource('/info/me.json', {}, {
        query: {method: 'GET'}
    });
}]);
