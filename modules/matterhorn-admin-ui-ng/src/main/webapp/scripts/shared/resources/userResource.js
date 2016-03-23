angular.module('adminNg.resources')
.factory('UserResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/users/:username.json', { }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
            return JSON.parse(data);
          }
        },
        update: {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              var parameters = {
                  username : data.username,
                  name     : data.name,
                  email    : data.email,
                  password : data.password
              };

              if (angular.isDefined(data.roles)) {
                parameters.roles = angular.toJson(data.roles);
              }

              return $.param(parameters);
          }
        }
    });
}]);
