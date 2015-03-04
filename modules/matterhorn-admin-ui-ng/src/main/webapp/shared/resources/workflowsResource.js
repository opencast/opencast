angular.module('adminNg.resources')
.factory('WorkflowsResource', ['$resource', function ($resource) {
    return $resource('/workflow/definitions.json', {}, {
        get: {method: 'GET', isArray: true, transformResponse: function (data) {
            var parsed = JSON.parse(data);
            if (parsed && parsed.definitions && parsed.definitions.definition) {
                return parsed.definitions.definition;
            }
            return [];
        }}
    });
}]);
