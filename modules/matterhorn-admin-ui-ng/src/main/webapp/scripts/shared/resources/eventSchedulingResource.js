angular.module('adminNg.resources')
.factory('EventSchedulingResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    var transformResponse = function (data) {
            var parsedData,
                startDate,
                endDate,
                duration,
                durationHours,
                durationMinutes;

            try {
                parsedData = JSON.parse(data);
            } catch (exception) {
                // this happens, e.g. "The resource you requested does not exist."
                return data;
            }

            startDate = new Date(parsedData.start);
            endDate = new Date(parsedData.end);
            duration = (endDate - startDate) / 1000;
            durationHours = (duration - (duration % 3600)) / 3600;
            durationMinutes = (duration % 3600) / 60;   


            parsedData.start = JsHelper.zuluTimeToDateObject(startDate);
            parsedData.end = JsHelper.zuluTimeToDateObject(endDate);
            parsedData.duration = {
                hour: durationHours,
                minute: durationMinutes
            };

            return parsedData;
        },
        transformRequest = function (data) {
            var result = data,
                start,
                end;

            if (angular.isDefined(data)) {
                start = JsHelper.toZuluTimeString(data.entries.start);
                end = JsHelper.toZuluTimeString(data.entries.start, data.entries.duration);
                result = $.param({scheduling: angular.toJson({
                    agentId: data.entries.agentId,
                    start: start,
                    end: end,
                    agentConfiguration: data.entries.agentConfiguration,
                })});
            }

            return result;
        };


    return $resource('/admin-ng/event/:id/scheduling:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        },
        save: {
            method: 'PUT',
            responseType: 'text',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: transformRequest
        }
    });
}]);
