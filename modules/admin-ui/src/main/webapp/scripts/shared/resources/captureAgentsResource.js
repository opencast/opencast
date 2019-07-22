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
.factory('CaptureAgentsResource', ['$resource', 'Language', function ($resource, Language) {
  return $resource('/admin-ng/capture-agents/:target', {}, {
    query: {
      method: 'GET',
      isArray: false,
      cancellable: true,
      params: {target: 'agents.json'},
      transformResponse: function (json) {
        var result = [], i = 0, parse, data;
        data = JSON.parse(json);

        parse = function (r) {
          var row = {};
          row.id = r.Name;
          row.status = r.Status;
          row.name = r.Name;
          row.updated = Language.formatDateTime('short', r.Update);
          row.inputs = r.inputs;
          row.roomId = r.roomId;
          row.type = 'LOCATION';
          row.removable = 'AGENTS.STATUS.OFFLINE' === r.Status || 'AGENTS.STATUS.UNKNOWN' === r.Status;
          return row;
        };

        if (!data) {
          return;
        }

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
