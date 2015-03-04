angular.module('adminNg.resources')
.factory('EventAttachmentsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/attachments.json', { id: '@id' }, {
        get: { method: 'GET', isArray: true }
    });
}]);
