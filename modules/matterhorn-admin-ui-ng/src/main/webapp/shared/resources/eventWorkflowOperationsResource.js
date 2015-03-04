angular.module('adminNg.resources')
.factory('EventWorkflowOperationsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var operations = {};
        try {
            operations = JSON.parse(data);
        } catch (e) { }
        return { entries: operations };
    };

    /* FIXME Christoph: Change endpoints!!! */
    return $resource('/admin-ng/event/:id0/workflows/:id1/operations.json', {}, {
        get: { method: 'GET', transformResponse: transform },
        save: { method: 'POST', transformRequest: function (data) {
            return JSON.stringify(data.entries);
        }, transformResponse: transform }
    });
}]);
