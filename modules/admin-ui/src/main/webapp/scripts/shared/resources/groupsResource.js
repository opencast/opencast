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
.factory('GroupsResource', ['$resource', 'ResourceHelper', function ($resource, ResourceHelper) {
  return $resource('/admin-ng/groups/:ext', {}, {
    query: {
      method: 'GET',
      params: { ext: 'groups.json' },
      isArray: false,
      cancellable: true,
      transformResponse: function (json) {
        return ResourceHelper.parseResponse(json, function (r) {
          var row = {};
          row.id = r.id;
          row.description = r.description;
          row.name = r.name;
          row.role = r.role;
          row.type = 'GROUP';
          return row;
        });
      }
    },
    create: {
      params: { ext: '' },
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {
        if (angular.isUndefined(data)) {
          return data;
        }

        var parameters = {
          name  : data.name
        };

        if (angular.isDefined(data.description)) {
          parameters.description = data.description;
        }

        if (angular.isDefined(data.roles)) {
          parameters.roles = data.roles.join(',');
        }

        if (angular.isDefined(data.users)) {
          parameters.users = data.users.join(',');
        }

        return $.param(parameters);
      }
    }
  });
}]);
