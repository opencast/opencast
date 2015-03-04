angular.module('adminNg.resources')
.factory('EventMediaDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/media/:id1.json');
}]);
