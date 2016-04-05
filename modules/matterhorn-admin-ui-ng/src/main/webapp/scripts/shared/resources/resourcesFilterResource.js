angular.module('adminNg.resources')
.factory('ResourcesFilterResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/resources/:resource/filters.json', {}, {
        get: { method: 'GET', transformResponse: function (data) {
            var filters = {};
            try {
                filters = JSON.parse(data);
            } catch (e) { }
            return { filters: filters };
        }}
    });
}]);
