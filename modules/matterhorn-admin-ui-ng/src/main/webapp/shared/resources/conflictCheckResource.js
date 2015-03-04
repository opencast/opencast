angular.module('adminNg.resources')
.factory('ConflictCheckResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    var transformRequest = function (data) {
        var endOfFirstLecture, endDate, result = {
            start: JsHelper.toZuluTimeString(data.start),
            device: data.device.id
        };
        result.duration = (
            parseInt(data.duration.hour, 10) * 60 * 60 * 1000 +
            parseInt(data.duration.minute, 10) * 60 * 1000
        ).toString();
        if (data.weekdays) {
            endOfFirstLecture = JsHelper.toZuluTimeString(data.start, data.duration);
            endDate = JsHelper.toZuluTimeString(data.end);
            result.end = endDate.substr(0, 11) + endOfFirstLecture.substr(11, 9);
            result.rrule = JsHelper.assembleRrule(data);
        }
        else {
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
