angular.module('adminNg.resources')
.factory('EventWorkflowsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var workflows = {};
        try {
            workflows = JSON.parse(data);
        } catch (e) { }

        if (angular.isDefined(workflows.results)) {
            return {
                        entries: workflows.results,
                        scheduling: false
                    };
        } else {
            return {
                        workflow: workflows,
                        scheduling: true
            };
        }
    };

    return $resource('/admin-ng/event/:id/workflows:ext', { id: '@id' }, {
        get: {
          params: { ext: '.json' },
          method: 'GET',
          transformResponse: transform
        },
        save: { 
            method: 'PUT',
            responseType: 'text',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                return $.param({configuration: angular.toJson(data.entries)});   
            }
        }
    });
}]);
