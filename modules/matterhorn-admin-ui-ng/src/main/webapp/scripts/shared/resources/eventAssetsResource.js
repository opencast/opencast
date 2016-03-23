angular.module('adminNg.resources')
.factory('EventAssetsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/asset/assets.json', { id: '@id' }, {
        get: { 
            method: 'GET', 
            isArray: false
        }
    });
}]);
