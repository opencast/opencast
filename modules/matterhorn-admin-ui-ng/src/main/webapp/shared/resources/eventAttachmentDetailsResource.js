angular.module('adminNg.resources')
.factory('EventAttachmentDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/attachment/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);
