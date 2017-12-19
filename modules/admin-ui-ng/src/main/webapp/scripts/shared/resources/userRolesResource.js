angular.module('adminNg.resources')
.factory('UserRolesResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/resources/ROLES.json', {}, {
        query: {
          method: 'GET',
          isArray: true,
          transformResponse: function (data) {
            var result = [];

            data = JSON.parse(data);

            if (angular.isDefined(data)) {
              angular.forEach(data, function(value, key) {
                result.push({
                  name: key,
                  value: key,
                  type: value
                });
              });
            }

            return result;
          }
        }
    });
}]);
