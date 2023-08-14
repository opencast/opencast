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
.factory('EventsSchedulingResource', ['$resource', 'JsHelper', '$httpParamSerializer',
  function ($resource, JsHelper, $httpParamSerializer) {
    var transformRequest = function (data) {
      return $httpParamSerializer(data);
    };
    var transformSingle = function (parsedData) {
      // Better deep-copy our response, testing frameworks use immutable structures.
      var data = $.extend(true, {}, parsedData);

      var startDate = new Date(parsedData.start);
      var endDate = new Date(parsedData.end);
      var duration = (endDate - startDate) / 1000;
      var durationHours = (duration - (duration % 3600)) / 3600;
      var durationMinutes = (duration % 3600) / 60;


      data.start = JsHelper.zuluTimeToDateObject(startDate);
      data.end = JsHelper.zuluTimeToDateObject(endDate);
      data.duration = {
        hour: durationHours,
        minute: durationMinutes
      };

      return data;
    };
    var transformResponse = function (parsedData) {
      var result = [];
      angular.forEach(parsedData, function(value) {
        result.push(transformSingle(value));
      });
      return result;
    };

    return $resource('/admin-ng/event/scheduling.json', {}, {
      bulkGet: {
        method: 'POST',
        responseType: 'json',
        isArray: true,
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
        },
        transformRequest: transformRequest,
        transformResponse: transformResponse
      }
    });
  }]);
