angular.module('adminNg.resources')
.factory('EventPublicationDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/publication/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);
