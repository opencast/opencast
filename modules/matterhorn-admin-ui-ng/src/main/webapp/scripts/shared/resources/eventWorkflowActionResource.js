angular.module('adminNg.resources')
.factory('EventWorkflowActionResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/workflow/action/:action', { id: '@id', action: '@action' }, {
    	save: {method:'PUT'}
    });
}]);
