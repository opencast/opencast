angular.module('adminNg.resources')
.factory('EventMediaDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/media/:id2.json', {}, {
        get: { method: 'GET', isArray: false, transformResponse: function (data) {
                var metadata = data;
                try {
                    metadata = JSON.parse(data);
                    metadata.video = {previews: [{uri: metadata.url}]};
                } catch (e) {
                    console.warn('Unable to parse JSON file: ' + e);
                }

                return metadata;
            }
        }
    });
}]);
