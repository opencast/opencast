angular.module('adminNg.resources')
.factory('GroupResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/groups/:id', { id: '@id' }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
            return JSON.parse(data);
          }
        },
        save: {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              var parameters = {
                name  : data.name
              };

              if (angular.isDefined(data.description)) {
                parameters.description = data.description;
              }

              if (angular.isDefined(data.roles)) {
                parameters.roles = data.roles.join(',');
              }

              if (angular.isDefined(data.users)) {
                parameters.users = data.users.join(',');
              }

              return $.param(parameters);
        }
      }
    });
}]);
