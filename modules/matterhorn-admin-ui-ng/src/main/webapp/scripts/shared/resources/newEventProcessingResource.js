angular.module('adminNg.resources')
.factory('NewEventProcessingResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var result = [];
        try {
        	result = JSON.parse(data);
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/event/new/processing', {}, {
        get: { method: 'GET', isArray: true, transformResponse: transform }
    });
}]);
