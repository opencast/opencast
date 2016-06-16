angular.module('adminNg.resources')
.factory('ServiceResource', ['$resource', function ($resource) {
    return $resource('/services/:resource', {}, {
        // Parameters:
        // * host: The host name, including the http(s) protocol
        // * maintenance: Whether this host should be put into maintenance mode (true) or not
        setMaintenanceMode: {
            method: 'POST',
            params: { resource: 'maintenance' },
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }
                return $.param(data);
            }
        },

        // Parameters:
        // * host: The host providing the service, including the http(s) protocol
        // * serviceType: The service type identifier
        sanitize: {
            method: 'POST',
            params: { resource: 'sanitize' },
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
