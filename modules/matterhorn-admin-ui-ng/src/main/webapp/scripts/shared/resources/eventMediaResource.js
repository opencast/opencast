angular.module('adminNg.resources')
.factory('EventMediaResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var media = [];
        try {
            media = JSON.parse(data);

            //for every media file item we define the filename
            for(var i = 0; i < media.length; i++){
                var item = media[i];
                var url = item.url;
                item.mediaFileName = url.substring(url.lastIndexOf('/')+1).split('?')[0];
            }

        } catch (e) { }
        return media;
    };

    return $resource('/admin-ng/event/:id0/asset/media/media.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id'}, transformResponse: transform }
    });
}]);
