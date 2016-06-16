angular.module('adminNg.resources')
.factory('EventErrorDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/workflows/:id1/errors/:id2.json');
}]);

