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
.factory('AclsResource', ['ResourceHelper', '$resource', function (ResourceHelper, $resource) {
  return $resource('/admin-ng/acl/:ext', {}, {
    query: {
      params: { ext: 'acls.json' },
      method: 'GET',
      isArray: false,
      cancellable: true,
      transformResponse: function (data) {
        return ResourceHelper.parseResponse(data, function (r) {
          var row = {};
          row.id      = r.id;
          row.name    = r.name;
          row.created = 'TBD';
          row.creator = 'TBD';
          row.in_use  = 'TBD';
          row.type    = 'ACL';

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

        return $.param({
          name : data.name,
          acl  : JSON.stringify({acl: data.acl})
        });
      }
    }
  });
}]);
