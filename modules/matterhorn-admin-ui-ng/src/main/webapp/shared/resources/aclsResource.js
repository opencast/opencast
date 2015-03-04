angular.module('adminNg.resources')
.factory('AclsResource', ['ResourceHelper', '$resource', function (ResourceHelper, $resource) {
    return $resource('/admin-ng/acl/:ext', {}, {
        query: {
            params: { ext: 'acls.json' },
            method: 'GET',
            isArray: false,
            transformResponse: function (data) {
              return ResourceHelper.parseResponse(data, function (r) {
                  var row = {};
                  row.id      = r.id;
                  row.name    = r.name;
                  row.created = 'TBD';
                  row.creator = 'TBD';
                  row.in_use  = 'TBD';

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

              return $.param({
                  name : data.name,
                  acl  : JSON.stringify({acl: data.acl})
              });
          }
        }
    });
}]);
