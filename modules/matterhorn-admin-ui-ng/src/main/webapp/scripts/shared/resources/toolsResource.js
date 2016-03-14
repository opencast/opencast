angular.module('adminNg.resources')
.factory('ToolsResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    return $resource('/admin-ng/tools/:id/:tool.json', { id: '@id' }, {
        get: {
            method: 'GET',
            transformResponse: function (json) {
                var data = JSON.parse(json);

                // Create a default segment spanning the entire track
                if (data.segments.length === 0) {
                    data.segments.push({
                        start: 0, end: data.duration
                    });
                }

                // Sort segments by start attribute
                data.segments.sort(function (a, b) {
                    return a.start - b.start;
                });

                // Fill gap until the first segment
                if (data.segments[0].start > 0) {
                    data.segments.splice(0, 0, {
                        start: 0,
                        end:   data.segments[0].start,
                        deleted: true
                    });
                }
                // Fill gap behind the last segment
                if (data.segments[data.segments.length - 1].end < data.duration) {
                    data.segments.splice(data.segments.length, 0, {
                        start: data.segments[data.segments.length - 1].end,
                        end:   data.duration,
                        deleted: true
                    });
                }
                // Fill gaps between segments
                angular.forEach(data.segments, function (segment, index) {
                    var previous = data.segments[index - 1];
                    if (previous && previous.end < segment.start - 1) {
                        data.segments.splice(index, 0, {
                            start: previous.end,
                            end: segment.start,
                            deleted: true
                        });
                    }
                });

                // Select first segment by default
                data.segments[0].selected = true;

                // Add workflow deselector
                if (data.workflows && data.workflows.length) {
                    data.workflows.splice(0, 0, { name: 'No Workflow' });
                }

                return data;
            }
        },
        save: {
            method: 'POST',
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }

                var response = {}, segments = [];
                angular.forEach(data.segments, function (segment) {
                    delete segment.$$hashKey;
                    if (!segment.deleted) {
                        this.push(segment);
                    }
                }, segments);

                response.concat = {
                    segments: segments,
                    tracks:   JsHelper.map(data.tracks, 'id')
                };

                if (data.workflow) {
                    response.workflow = data.workflow;
                }

                return JSON.stringify(response);
            }
        }
    });
}]);
