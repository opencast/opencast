angular.module('adminNg.resources')
.factory('OptoutSingleResource', ['$resource', function ($resource) {

    return $resource('/admin-ng/:resource/:id/optout/:optout', {resource: '@resource', id: '@id', optout: '@optout'}, {
        save: {
            method: 'PUT'
        }
    });
}]);

