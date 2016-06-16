angular.module('adminNg.resources')
.factory('EventErrorsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var errors = {};
        try {
            errors = JSON.parse(data);
        } catch (e) { }
        return { entries: errors };
    };

    return $resource('/admin-ng/event/:id0/workflows/:id1/errors.json', {}, {
        get: { method: 'GET', transformResponse: transform }
    });
}]);
