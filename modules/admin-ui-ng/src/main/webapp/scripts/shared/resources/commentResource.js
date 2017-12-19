angular.module('adminNg.resources')
.factory('CommentResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/:resource/:resourceId/:type/:id/:reply', {}, {
        save: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }
                return $.param(data);
            }
        }
    });
}]);
