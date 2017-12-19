angular.module('adminNg.resources')
.factory('EventTransactionResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var result = {};
        try {
              result.hasActiveTransaction = JSON.parse(data).active;
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/event/:id/hasActiveTransaction', {}, {
        hasActiveTransaction: { 
            method: 'GET',
            responseType: 'text',
            isArray: false, 
            paramDefaults: { id: '@id'},
            transformResponse: transform
        }
    });
}]);
