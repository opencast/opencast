angular.module('adminNg.resources')
.factory('SeriesParticipationResource', ['$resource', function ($resource) {
    var transformResponse = function (data) {
        var metadata = {};

        try {
            metadata = JSON.parse(data);
            if (metadata.opt_out) {
                metadata.opt_out = 'true';
            } else {
                metadata.opt_out = 'false';
            }
        } catch (e) { }

        return metadata;
    };

    return $resource('/admin-ng/series/:id/participation:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        }
    });
}]);
