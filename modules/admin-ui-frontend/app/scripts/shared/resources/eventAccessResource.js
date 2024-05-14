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
.factory('EventAccessResource', ['$resource', function ($resource) {
  var transform = function (data) {
        var metadata = {};
        try {
          metadata = JSON.parse(data);
        } catch (e) { }
        return metadata;
      },
      eventResource,
      accessResource,
      managedAclResource;

  eventResource = $resource('/admin-ng/event/:id/access.json', { id: '@id' }, {
    get: { method: 'GET', transformResponse: transform }
  });

  accessResource = $resource('/admin-ng/event/:id/access', { id: '@id' }, {
    save: { method: 'POST',
      isArray: true,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {

        if (angular.isUndefined(data)) {
          return data;
        }

        return $.param({
          acl      : angular.toJson({acl: data.acl}),
          override : true
        });
      }
    }
  });

  managedAclResource = $resource('/acl-manager/acl/:id', { id: '@id'}, {
    get: { method: 'GET'}
  });


  return {
    getManagedAcl : managedAclResource.get,
    get: eventResource.get,
    save: accessResource.save
  };
}]);
