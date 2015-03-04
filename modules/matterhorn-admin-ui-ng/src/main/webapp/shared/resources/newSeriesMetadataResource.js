angular.module('adminNg.resources')
.factory('NewSeriesMetadataResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var metadata = {}, result = {};
        try {
            metadata = JSON.parse(data);
            angular.forEach(metadata, function (md) {
                result[md.flavor] = md;
                result[md.flavor].title = md.title;
            });
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/series/new/metadata', {}, {
        get: { method: 'GET', transformResponse: transform }
    });
}]);
