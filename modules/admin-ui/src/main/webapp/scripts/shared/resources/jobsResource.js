angular.module('adminNg.resources')
.factory('JobsResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/job/jobs.json', {}, {
        query: { method: 'GET', isArray: false, transformResponse: function (json) {
            var result = [], i = 0, parse, data;
            data = JSON.parse(json);

            parse = function (r) {
                var row = {};
                row.id = r.id
                row.operation = r.operation;
                row.type = r.type;
                row.status = r.status;
                row.submitted = Language.formatDateTime('short', r.submitted);
                row.started = Language.formatDateTime('short', r.started);
                row.creator = r.creator;
                row.processingHost = r.processingHost;
                return row;
            };

            for (; i < data.results.length; i++) {
                result.push(parse(data.results[i]));
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
