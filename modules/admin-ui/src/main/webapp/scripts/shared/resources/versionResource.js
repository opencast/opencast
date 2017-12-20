angular.module('adminNg.resources')
.factory('VersionResource', ['$resource', function ($resource) {
    return $resource('/sysinfo/bundles/version?prefix=opencast', {}, {
        query: { method: 'GET' }
    });
}]);
