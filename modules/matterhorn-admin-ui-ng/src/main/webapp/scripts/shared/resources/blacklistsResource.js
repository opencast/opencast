angular.module('adminNg.resources')
.factory('BlacklistsResource', ['$resource', 'Language', 'JsHelper',
function ($resource, Language, JsHelper) {

    var parse = function (r) {
        var row = {};
        row.id            = r.id;
        row.resourceName  = r.resource.name;
        row.resourceId    = r.resource.id;
        row.date_from     = Language.formatDateTime('short', r.start);
        row.date_to       = Language.formatDateTime('short', r.end);
        row.date_from_raw = Language.formatDateTimeRaw('short', r.start);
        row.date_to_raw   = Language.formatDateTimeRaw('short', r.end);
        row.reason        = r.purpose;
        row.comment       = r.comment;

        return row;
    };

    return function (type) {
        return {
            query: {
                method: 'GET',
                isArray: false,
                params: { type: type, id: 'blacklists.json' },
                transformResponse: function (data) {
                    var result = [];
                    angular.forEach(JSON.parse(data), function (item) {
                        result.push(parse(item));
                    });

                    return {
                        rows   : result,
                        total  : data.total,
                        offset : data.offset,
                        count  : data.count,
                        limit  : data.limit
                    };
                }
            },
            get: {
                method: 'GET',
                params: { type: type },
                transformResponse: function (data) {
                    return parse(JSON.parse(data));
                }
            },
            save: {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {
                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    var request = {};

                    request.type = type;
                    if (data.items.items.length) {
                        request.blacklistedId = data.items.items[0].id;
                    }
                    request.start = JsHelper.toZuluTimeString({
                        date: data.dates.fromDate,
                        hour: data.dates.fromTime.split(':')[0],
                        minute: data.dates.fromTime.split(':')[1]
                    });
                    request.end = JsHelper.toZuluTimeString({
                        date: data.dates.toDate,
                        hour: data.dates.toTime.split(':')[0],
                        minute: data.dates.toTime.split(':')[1]
                    });
                    request.purpose = data.reason.reason;
                    request.comment = data.reason.comment;

                    return $.param(request);
                }
            },
            update: {
                method: 'PUT',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {
                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    var request = {};

                    request.type = type;
                    if (data.items.items.length) {
                        request.blacklistedId = data.items.items[0].id;
                    }
                    request.start = JsHelper.toZuluTimeString({
                        date: data.dates.fromDate,
                        hour: data.dates.fromTime.split(':')[0],
                        minute: data.dates.fromTime.split(':')[1]
                    });
                    request.end = JsHelper.toZuluTimeString({
                        date: data.dates.toDate,
                        hour: data.dates.toTime.split(':')[0],
                        minute: data.dates.toTime.split(':')[1]
                    });
                    request.purpose = data.reason.reason;
                    request.comment = data.reason.comment;

                    return $.param(request);
                }
            }
        };
    };
}])
.factory('UserBlacklistsResource', ['$resource', 'BlacklistsResource',
function ($resource, BlacklistsResource) {
    return $resource('/blacklist/:id', {}, BlacklistsResource('person'));
}])
.factory('LocationBlacklistsResource', ['$resource', 'BlacklistsResource',
function ($resource, BlacklistsResource) {
    return $resource('/blacklist/:id', {}, BlacklistsResource('room'));
}]);
