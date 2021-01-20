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
.factory('ResourcesFilterResource', ['$resource', function ($resource) {
  return $resource('/admin-ng/resources/:resource/filters.json', {}, {
    get: { method: 'GET', transformResponse: function (data) {
      var filters = {};
      try {
        filters = JSON.parse(data);
        for (var key in filters) {
          if (!filters[key].options) {
            continue;
          }
          var filterArr = [];
          var options = filters[key].options;
          for (var subKey in options) {
            filterArr.push({value: subKey, label: options[subKey]});
          }
          filterArr = filterArr.sort(function(a, b) {
            if (a.label.toLowerCase() < b.label.toLowerCase()) return -1;
            if (a.label.toLowerCase() > b.label.toLowerCase()) return 1;
            return 0;
          });
          filters[key].options = filterArr;
        }
      } catch (e) { }
      return { filters: filters };
    }}
  });
}]);
