angular.module('adminNg.resources')
.factory('EventPublicationsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/publication/publications.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id' } }
    });
}]);
