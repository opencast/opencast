angular.module('adminNg.resources')
.factory('SeriesResource', ['$resource', 'Language', 'ResourceHelper', function ($resource, Language, ResourceHelper) {

    return $resource('/admin-ng/series/:target', {}, {
        query: {
            method: 'GET',
            params: { target: 'series.json' },
            isArray: false,
            transformResponse: function (data) {
                return ResourceHelper.parseResponse(data, function (r) {
                    var row = {};
                    row.id = r.id;
                    row.title = r.title;
                    row.creator = r.organizers.join(', ');
                    row.contributors = r.contributors.join(', ');
                    row.createdDateTime = Language.formatDate('short', r.creation_date);
                    row.managed_acl = r.managedAcl;
                    return row;
                });
            }
        },
        create: {
            method: 'POST',
            params: { target: 'new' },
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }

                return $.param({metadata: angular.toJson(data)});
            },
            transformResponse: function (response) {
                // if this method is missing, the angular default is to interpret the response as JSON
                // in our case, the response is just a uuid string which causes angular to break.
                return response;
            }
        }
    });
}]);
