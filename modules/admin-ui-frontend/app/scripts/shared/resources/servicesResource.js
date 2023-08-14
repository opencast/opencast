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
.factory('ServicesResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
  // Note: This is the productive path
  return $resource('/admin-ng/services/services.json', {}, {
    query: { method: 'GET', isArray: false, cancellable: true, transformResponse: function (data) {
      var result = [], i = 0, parse, payload;
      data = JSON.parse(data);
      payload = data.results;

      parse = function (r) {
        r.action = '';

        r.meanRunTime = JsHelper.secondsToTime(r.meanRunTime);
        r.meanQueueTime = JsHelper.secondsToTime(r.meanQueueTime);

        return r;
      };

      for (; i < payload.length; i++) {
        result.push(parse(payload[i]));
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
