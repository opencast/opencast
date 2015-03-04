angular.module('adminNg.resources')
.factory('EventWorkflowOperationDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/workflows/:id1/operations/:id2.json');
}]);
