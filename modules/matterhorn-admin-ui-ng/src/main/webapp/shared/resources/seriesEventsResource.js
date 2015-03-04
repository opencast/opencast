angular.module('adminNg.resources')
.factory('SeriesEventsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var events = {};
        try {
            events = JSON.parse(data);
        } catch (e) { }
        return { entries: events };
    };

    return $resource('/admin-ng/series/:id/events.json', { id: '@id' }, {
        get: { method: 'GET', transformResponse: transform },
        save: { method: 'PUT', transformRequest: function (data) {
            return JSON.stringify(data.entries);
        }, transformResponse: transform }
    });
}]);
