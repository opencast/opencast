angular.module('adminNg.resources')
.factory('EventGeneralResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/general.json', { id: '@id' });
}]);
