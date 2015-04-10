angular.module('adminNg.resources')
.factory('ConflictCheckResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    var transformRequest = function (data) {
        var result = {
                start: JsHelper.toZuluTimeString(data.start),
                device: data.device.id,
                duration: String(moment.duration(parseInt(data.duration.hour, 10), 'h')
                            .add(parseInt(data.duration.minute, 10), 'm')
                            .asMilliseconds())
            };

        if (data.weekdays) {
            result.end = JsHelper.toZuluTimeString({
                date: data.end,
                hour: data.start.hour,
                minute: data.start.minute
            }, data.duration);
            result.rrule = JsHelper.assembleRrule(data);
        } else {
            result.end = JsHelper.toZuluTimeString(data.start, data.duration);
        }

        return $.param({metadata: angular.toJson(result)});
    };

    return $resource('/admin-ng/event/new/conflicts', {}, {
        check: { method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: transformRequest }
    });
}]);
