angular.module('adminNg.resources')
.factory('ResourcesListResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/resources/:resource.json', {}, {
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
                  value: value
                });
              });
            }

            return result;
          }
        }
      });
}]);
