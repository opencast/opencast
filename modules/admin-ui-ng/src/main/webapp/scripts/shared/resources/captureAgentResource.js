angular.module('adminNg.resources')
.factory('CaptureAgentResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/capture-agents/:name', { name: '@name' }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
              var raw = JSON.parse(data), agent = {}
              agent.name = raw.Name;
              agent.url = raw.URL;
              agent.status = raw.Status;
              agent.updated =  Language.formatDateTime('short', raw.Update);
              agent.inputs = raw.inputs;
              agent.capabilities = raw.capabilities;
              agent.configuration = raw.configuration;
              return agent;
          }
        }
      }
    );
}]);
