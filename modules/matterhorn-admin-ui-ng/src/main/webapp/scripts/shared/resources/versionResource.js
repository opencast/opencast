angular.module('adminNg.resources')
.factory('VersionResource', ['$resource', function ($resource) {
    return $resource('/sysinfo/bundles/version?prefix=matterhorn', {}, {
        query: { method: 'GET' }
    });
}]);
