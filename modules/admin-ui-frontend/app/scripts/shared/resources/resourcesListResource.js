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
.factory('ResourcesListResource', ['$resource', function ($resource) {
  return $resource('/admin-ng/resources/:resource.json', {}, {
    query: { // for parsing lists where the value is a string
      method: 'GET',
      isArray: true,
      transformResponse: function (data) {
        var result = [];

        data = JSON.parse(data);

        if (angular.isDefined(data)) {
          angular.forEach(data, function(value, key) {

            result.push({
              name: key,
              value: value
            });

          });
        }

        return result;
      }
    },
    queryRecursive: { // for parsing lists where the value is json
      method: 'GET',
      isArray: true,
      transformResponse: function (data) {

        var result = [];

        data = JSON.parse(data);

        if (angular.isDefined(data)) {
          angular.forEach(data, function(value, key) {

            var jsonValue = JSON.parse(value);

            result.push({
              name: key,
              value: jsonValue
            });

          });
        }

        return result;
      }
    }
  });
}]);
