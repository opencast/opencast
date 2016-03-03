angular.module('adminNg.resources')
.factory('OptoutsResource', ['$resource', function ($resource) {
    var transformRequest = function (data) {
        if (angular.isDefined(data.eventIds)) {
            data.eventIds = '["' + data.eventIds.join('","') + '"]';
        } else if (angular.isDefined(data.seriesIds)) {
            data.seriesIds = '["' + data.seriesIds.join('","') + '"]';
        }
        return $.param(data);
    };
    return $resource('/admin-ng/:resource/optouts', {resource: '@resource'}, {
        save: {
            method: 'POST',
            responseType: 'json',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            transformRequest: transformRequest
        }
    });
}]);

