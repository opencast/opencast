/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
'use strict';

angular.module('adminNg.resources')
.factory('ToolsResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    var transformResponseFunction = function (json) {
        if (!json) {
            return json;
        }

        var data = JSON.parse(json);

        // Create a default segment spanning the entire track
        if (data.segments.length === 0) {
            data.segments.push({
                start: 0, end: +data.duration
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
        //dont use angular.forEach here see MH-11169
        for (var index = 0; index < data.segments.length; index++) {
            var previous = data.segments[index - 1];
            var segmentStart = data.segments[index].start - 1;
            if (previous && previous.end < segmentStart) {
                data.segments.splice(index, 0, {
                    start: previous.end,
                    end: data.segments[index].start,
                    deleted: true
                });
            }
        };
        // Select first segment by default
        data.segments[0].selected = true;

        // Add workflow deselector
        if (data.workflows && data.workflows.length) {
            data.workflows.splice(0, 0, { name: 'No Workflow' });
        }

        // Reasonable default in case there is no information about thumbnails available
        if (!data.thumbnail) {
          data.thumbnail = {};
          data.thumbnail['type'] = "DEFAULT";
        }

        return data;
    };
    return $resource('/admin-ng/tools/:id/:tool.json', { id: '@id' }, {
        get: {
            method: 'GET',
            transformResponse: transformResponseFunction
        },
        thumbnail: {
            method: 'POST',
            headers: { 'Content-Type': undefined},
            transformRequest: function (data) {
                var fd = new FormData();
                if (data.file) fd.append('File', data.file);
                if (data.track) fd.append('Track', data.track);
                if (angular.isNumber(data.position)) fd.append('Position', data.position);
                return fd;
            }
        },
        save: {
            method: 'POST',
            transformResponse: transformResponseFunction,
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

                if (data.thumbnail && data.thumbnail.defaultThumbnailPositionChanged) {
                    response.defaultThumbnailPosition = data.thumbnail.position;
                }

                return JSON.stringify(response);
            }
        }
    });
}]);
