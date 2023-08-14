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
        date: data.end.date,
        hour: data.start.hour,
        minute: data.start.minute
      }, data.duration);
      result.rrule = JsHelper.assembleRrule(data);
    } else {
      result.end = JsHelper.toZuluTimeString(data.start, data.duration);
    }

    if (data.eventId) {
      result.id = data.eventId;
    }

    return $.param({metadata: angular.toJson(result)});
  };

  return $resource('/admin-ng/event/new/conflicts', {}, {
    check: { method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: transformRequest }
  });
}]);
