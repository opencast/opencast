angular.module('adminNg.resources')
.factory('OptoutsResource', ['$resource', function ($resource) {
    var transformRequest = function (data) {
        data.eventIds = '["' + data.eventIds.join('","') + '"]';
        return $.param(data);
    };
    return $resource('/admin-ng/:resource/optouts', {resource: '@resource'}, {
        save: {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            transformRequest: transformRequest
        }
    });
}]);

