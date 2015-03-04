angular.module('adminNg.resources')
.factory('EventWorkflowsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var workflows = {};
        try {
            workflows = JSON.parse(data);
        } catch (e) { }
        return { entries: workflows.results };
    };

    return $resource('/admin-ng/event/:id/workflows:ext', { id: '@id' }, {
        get: {
          params: { ext: '.json' },
          method: 'GET',
          transformResponse: transform
        },
        save: { method: 'POST', transformRequest: function (data) {
            return JSON.stringify(data.entries);
        }, transformResponse: transform }
    });
}]);
