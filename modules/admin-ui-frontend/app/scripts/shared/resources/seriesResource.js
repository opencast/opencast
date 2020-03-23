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
.factory('SeriesResource', ['$resource', 'Language', 'ResourceHelper', function ($resource, Language, ResourceHelper) {

  return $resource('/admin-ng/series/:id', { id: '@id' }, {
    query: {
      method: 'GET',
      params: { id: 'series.json' },
      isArray: false,
      cancellable: true,
      transformResponse: function (data) {
        return ResourceHelper.parseResponse(data, function (r) {
          var row = {};
          row.id = r.id;
          row.title = r.title;
          row.creators = r.organizers;
          row.contributors = r.contributors;
          row.createdDateTime = Language.formatDate('short', r.creation_date);
          row.managed_acl = r.managedAcl;
          row.type = 'SERIES';
          return row;
        });
      }
    },
    create: {
      method: 'POST',
      params: { id: 'new' },
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {
        if (angular.isUndefined(data)) {
          return data;
        }

        return $.param({metadata: angular.toJson(data)});
      },
      transformResponse: function (response) {
        // if this method is missing, the angular default is to interpret the response as JSON
        // in our case, the response is just a uuid string which causes angular to break.
        return response;
      }
    }
  });
}]);
