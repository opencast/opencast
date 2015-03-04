angular.module('adminNg.resources')
.factory('EventMediaResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var media = {};
        try {
            media = JSON.parse(data);

            //for every media file item we define the filename
            for(var i = 0; i < media.length; i++){
                var item = media[i];
                var url = item.url;
                item.mediaFileName = url.substring(url.lastIndexOf('/')+1);
            }

        } catch (e) { }
        return { entries: media };
    };

    return $resource('/admin-ng/event/:id/media.json', { id: '@id' }, {
        get: { method: 'GET', transformResponse: transform },
        save: { method: 'POST', transformRequest: function (data) {
            return JSON.stringify(data.entries);
        }, transformResponse: transform }
    });
}]);
