angular.module('adminNg.resources')
.factory('SeriesThemeResource', ['$resource', function ($resource) {

        return $resource('/admin-ng/series/:id/theme:ext', {id: '@id'}, {
            save: {
            	method: 'PUT',
            	headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                transformRequest: function (data) {
                    var d = {};
                    if (angular.isUndefined(data)) {
                        return data;
                    }
                    d.themeId = data.theme;
                	return $.param(d);	
            	}
            },
            get: {params:{'ext':'.json'}, method: 'GET'},
            delete: {params:{'ext':''}, method: 'DELETE'}
        });
}]);
