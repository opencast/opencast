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
.factory('EventSchedulingResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
  var transformResponse = function (data, headers, status) {

        // an event may have no schedule
        if (status == 404) {
          return;
        }

        var parsedData,
            startDate,
            endDate,
            duration,
            durationHours,
            durationMinutes;

        parsedData = JSON.parse(data);

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
