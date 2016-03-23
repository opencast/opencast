angular.module('adminNg.resources')
.factory('EventCatalogDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/catalog/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);
