angular.module('adminNg.resources')
.factory('EventWorkflowDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/workflows/:id1.json');
}]);
