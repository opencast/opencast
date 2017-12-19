angular.module('adminNg.resources')
.factory('UsersResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/users/:target', {}, {
        query: {
            method: 'GET',
            isArray: false,
            params : { target: 'users.json' },
            transformResponse: function (data) {
                var result = [], i = 0, parse;
                data = JSON.parse(data);

                parse = function (r) {
                    var row = {};
                    row.id = (angular.isDefined(r.personId) && r.personId !== -1) ? r.personId : r.username;
                    row.name = r.name;
                    row.username = r.username;
                    row.manageable = r.manageable;
                    row.rolesDict = {};
                    var roleNames = [];
                    angular.forEach(r.roles, function(role) {
                        roleNames.push(role.name);
                        row.rolesDict[role.name] = role;
                    });
                    row.roles = roleNames.join(", ");
                    row.provider = r.provider;
                    row.email = r.email;
                    if (!angular.isUndefined(r.blacklist)) {
                        row.blacklist_from = Language.formatDateTime('short', r.blacklist.start);
                        row.blacklist_to   = Language.formatDateTime('short', r.blacklist.end);
                    }
                    row.type = "USER";
                    return row;
                };

                for (i = 0; i < data.results.length; i++) {
                    result.push(parse(data.results[i]));
                }

                return {
                    rows   : result,
                    total  : data.total,
                    offset : data.offset,
                    count  : data.count,
                    limit  : data.limit
                };
            }
        },
        create: {
            params : { target: '' },
            method: 'POST',
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
