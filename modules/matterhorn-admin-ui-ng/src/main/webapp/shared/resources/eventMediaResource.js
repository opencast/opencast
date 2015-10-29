angular.module('adminNg.resources')
.factory('EventMediaResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/media/media.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id'} }
    });
}]);
