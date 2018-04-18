angular.module('adminNg.resources')
.factory('EventHasSnapshotsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/hasSnapshots.json', { id: '@id' });
}]);
