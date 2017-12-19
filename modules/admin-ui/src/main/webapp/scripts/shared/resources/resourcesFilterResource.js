angular.module('adminNg.resources')
.factory('ResourcesFilterResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/resources/:resource/filters.json', {}, {
        get: { method: 'GET', transformResponse: function (data) {
            var filters = {};
            try {
                filters = JSON.parse(data);
                for (var key in filters) {
                    if (!filters[key].options) {
                        continue;
                    }
                    var filterArr = [];
                    var options = filters[key].options;
                    for (var subKey in options) {
                        filterArr.push({value: subKey, label: options[subKey]});
                    }
                    filterArr = filterArr.sort(function(a, b) {
                                    if (a.label.toLowerCase() < b.label.toLowerCase()) return -1;
                                    if (a.label.toLowerCase() > b.label.toLowerCase()) return 1;
                                    return 0;
                                });
                    filters[key].options = filterArr;
                }
            } catch (e) { }
            return { filters: filters };
        }}
    });
}]);
