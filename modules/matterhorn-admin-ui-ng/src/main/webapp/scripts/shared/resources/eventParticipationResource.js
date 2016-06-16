angular.module('adminNg.resources')
.factory('EventParticipationResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
    var transformResponse = function (data) {
        var metadata = {};

        try {
            metadata = JSON.parse(data);
            if (!angular.isString(metadata.opt_out)) {
                if (metadata.opt_out) {
                    metadata.opt_out = 'true';
                } else {
                    metadata.opt_out = 'false';
                }
            } 
        } catch (e) { }

        return metadata;
    };

    return $resource('/admin-ng/event/:id/participation:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        }
    });
}]);
