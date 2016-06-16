angular.module('adminNg.resources')
.factory('ServersResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    return $resource('/admin-ng/server/servers.json', {}, {
        query: { method: 'GET', isArray: false, transformResponse: function (rawData) {
            var data = JSON.parse(rawData);
            var result = [];
            for (var i = 0; i < data.results.length; i++) {
                var row = data.results[i];
                row.id = row.name;

                row.meanRunTime = JsHelper.secondsToTime(row.meanRunTime);
                row.meanQueueTime = JsHelper.secondsToTime(row.meanQueueTime);

                result.push(row);
            }

            return {
                rows: result,
                total: data.total,
                offset: data.offset,
                count: data.count,
                limit: data.limit
            };
        }}
    });
}]);
