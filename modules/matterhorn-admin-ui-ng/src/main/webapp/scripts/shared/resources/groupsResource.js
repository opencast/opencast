angular.module('adminNg.resources')
.factory('GroupsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/groups/:ext', {}, {
        query: {
            method: 'GET',
            params: { ext: 'groups.json' },
            isArray: false,
            transformResponse: function (json) {
              var result = [], parse, data;
              data = JSON.parse(json);

              parse = function (r) {
                  var row = {};
                  row.id = r.id;
                  row.description = r.description;
                  row.name = r.name;
                  row.role = r.role;
                  return row;
              };

              angular.forEach(data.results, function (item) {
                  result.push(parse(item));
              });

              return {
                  rows   : result,
                  total  : result.lenght,
                  offset : 0,
                  count  : result.length,
                  limit  : 0
              };
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
