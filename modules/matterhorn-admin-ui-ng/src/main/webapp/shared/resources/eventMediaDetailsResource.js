angular.module('adminNg.resources')
.factory('EventMediaDetailsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var media = {};
        try {
            media = JSON.parse(data);
            media.url = media.url.split('?')[0];
        } catch (e) { }
        return media;
    };

    return $resource('/admin-ng/event/:id0/media/:id1.json', {}, {
        get: {method: 'GET', transformResponse: transform}
    });
}]);
