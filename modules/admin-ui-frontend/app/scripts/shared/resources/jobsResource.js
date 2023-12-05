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
.factory('JobsResource', ['$resource', 'Language', function ($resource, Language) {
  return $resource('/admin-ng/job/jobs.json', {}, {
    query: { method: 'GET', isArray: false, cancellable: true, transformResponse: function (json) {
      var result = [], i = 0, parse, data;
      data = JSON.parse(json);

      parse = function (r) {
        var row = {};
        row.id = r.id;
        row.operation = r.operation;
        row.type = r.type;
        row.status = r.status;
        row.submitted = Language.formatDateTime('short', r.submitted);
        row.started = Language.formatDateTime('short', r.started);
        row.creator = r.creator;
        row.processingHost = r.processingHost;
        row.processingNode = r.processingNode;
        return row;
      };

      for (; i < data.results.length; i++) {
        result.push(parse(data.results[i]));
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
