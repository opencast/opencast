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
.factory('UsersResource', ['$resource', 'Language', function ($resource, Language) {
  return $resource('/admin-ng/users/:target', {}, {
    query: {
      method: 'GET',
      isArray: false,
      cancellable: true,
      params : { target: 'users.json' },
      transformResponse: function (data) {
        var result = [], i = 0, parse;
        data = JSON.parse(data);

        parse = function (r) {
          var row = {};
          row.id = (angular.isDefined(r.personId) && r.personId !== -1) ? r.personId : r.username;
          row.name = r.name;
          row.username = r.username;
          row.manageable = r.manageable;
          row.rolesDict = {};
          var roleNames = [];
          angular.forEach(r.roles, function(role) {
            roleNames.push(role.name);
            row.rolesDict[role.name] = role;
          });
          row.roles = roleNames.join(', ');
          row.provider = r.provider;
          row.email = r.email;
          row.type = 'USER';
          return row;
        };

        if (!data) {
          return;
        }

        for (i = 0; i < data.results.length; i++) {
          result.push(parse(data.results[i]));
        }

        return {
          rows   : result,
          total  : data.total,
          offset : data.offset,
          count  : data.count,
          limit  : data.limit
        };
      }
    },
    create: {
      params : { target: '' },
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {
        if (angular.isUndefined(data)) {
          return data;
        }

        var parameters = {
          username : data.username,
          name     : data.name,
          email    : data.email,
          password : data.password
        };

        if (angular.isDefined(data.roles)) {
          parameters.roles = angular.toJson(data.roles);
        }

        return $.param(parameters);
      }
    }
  });
}]);
