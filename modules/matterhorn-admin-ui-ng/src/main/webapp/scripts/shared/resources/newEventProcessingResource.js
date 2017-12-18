angular.module('adminNg.resources')
.factory('NewEventProcessingResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var result = result = JSON.parse(data);
        return result;
    };

    return $resource('/admin-ng/event/new/processing', {}, {
        get: { method: 'GET', isArray: true, transformResponse: transform }
    });
}]);
