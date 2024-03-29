/*
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
.factory('SeriesAccessResource', ['$resource', function ($resource) {
  var seriesResource,
      managedAclResource,
      accessResource;

  seriesResource = $resource('/admin-ng/series/:id/access.json', { id: '@id' }, {
    get: {  method: 'GET' }
  });

  accessResource = $resource('/admin-ng/series/:id/access', { id: '@id' }, {
    save: { method: 'POST',
      isArray: true,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {

        if (angular.isUndefined(data)) {
          return data;
        }

        return $.param({
          acl      : angular.toJson({acl: data.acl}),
          override : data.override
        });
      }
    }
  });

  managedAclResource = $resource('/acl-manager/acl/:id', { id: '@id'}, {
    get: { method: 'GET'}
  });

  return {
    getManagedAcl : managedAclResource.get,
    get           : seriesResource.get,
    save          : accessResource.save,
  };
}]);
