angular.module('adminNg.resources')
.factory('ServicesResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    // Note: This is the productive path
    return $resource('/admin-ng/services/services.json', {}, {
        query: { method: 'GET', isArray: false, transformResponse: function (data) {
            var result = [], i = 0, parse, payload;
            data = JSON.parse(data);
            payload = data.results;

            parse = function (r) {
                r.action = '';

                r.meanRunTime = JsHelper.secondsToTime(r.meanRunTime);
                r.meanQueueTime = JsHelper.secondsToTime(r.meanQueueTime);

                return r;
            };

            for (; i < payload.length; i++) {
                result.push(parse(payload[i]));
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
