angular.module('adminNg.resources')
.factory('CaptureAgentsResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/capture-agents/agents.json', {}, {
        query: {method: 'GET', isArray: false, transformResponse: function (json) {
            var result = [], i = 0, parse, data;
            data = JSON.parse(json);

            parse = function (r) {
                var row = {};
                row.id = r.Name;
                row.status = r.Status;
                row.name = r.Name;
                row.updated = Language.formatTime('short', r.Update);
                row.inputs = r.inputs;
                row.roomId = r.roomId;
                row.blacklist_from = Language.formatDateTime('short', r.blacklist.start);
                row.blacklist_to   = Language.formatDateTime('short', r.blacklist.end);

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
