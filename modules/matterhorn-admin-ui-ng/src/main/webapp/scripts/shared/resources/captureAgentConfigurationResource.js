angular.module('adminNg.resources')
.factory('CaptureAgentConfigurationResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/capture-agents/:name/configuration.json', { name: '@name' }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
            return JSON.parse(data);
          }
        }
      }
    );
}]);
