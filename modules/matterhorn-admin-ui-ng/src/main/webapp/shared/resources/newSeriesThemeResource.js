angular.module('adminNg.resources')
.factory('NewSeriesThemeResource', ['$resource', function ($resource) {
    var transform = function (data) {
    	var result = {};
        try {
        	result = JSON.parse(data);
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/series/new/themes', {}, {
        get: { method: 'GET', isArray: false, transformResponse: transform }
    });
}]);
