angular.module('adminNg.resources')
.factory('GroupsResource', ['$resource', 'ResourceHelper', function ($resource, ResourceHelper) {
    return $resource('/admin-ng/groups/:ext', {}, {
        query: {
          method: 'GET',
          params: { ext: 'groups.json' },
          isArray: false,
          transformResponse: function (json) {
              return ResourceHelper.parseResponse(json, function (r) {
                  var row = {};
                  row.id = r.id;
                  row.description = r.description;
                  row.name = r.name;
                  row.role = r.role;
                  row.type = "GROUP";
                  return row;
              });
          }
        },
        create: {
          params: { ext: '' },
          method: 'POST',
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
