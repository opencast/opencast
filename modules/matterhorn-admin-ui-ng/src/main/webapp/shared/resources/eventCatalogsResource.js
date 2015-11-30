angular.module('adminNg.resources')
.factory('EventCatalogsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/catalog/catalogs.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id' } }
    });
}]);
