angular.module('adminNg.resources')
.factory('EventMediaDetailsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var media = {};
        try {
            if (typeof data === 'string') {
                media = JSON.parse(data);
            } else {
                media = data;
            }
            media.video = { previews: [{uri: media.url}] };
            media.url = media.url.split('?')[0];
        } catch (e) {
            console.warn('Unable to parse JSON file: ' + e);
        }
        return media;
    };

    return $resource('/admin-ng/event/:id0/asset/media/:id2.json', {}, {
        get: { method: 'GET', isArray: false, transformResponse: transform }
    });
}]);