angular.module('adminNg.resources')
.factory('EventWorkflowActionResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/workflows/:wfId/action/:action', { id: '@id', wfId: '@wfId', action: '@action' }, {
    	save: {method:'PUT'}
    });
}]);

