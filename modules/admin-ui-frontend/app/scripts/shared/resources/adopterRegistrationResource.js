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
.factory('AdopterRegistrationResource', ['$resource', function ($resource) {
  var get,
      create,
      finalize;

  get = $resource('/admin-ng/adopter/registration/', {}, {
    get: {
      method: 'GET',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    },
  });

  create = $resource('/admin-ng/adopter/registration/:target', {}, {
    post: {
      params : { target: '' },
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {
        if (angular.isUndefined(data)) {
          return data;
        }

        var parameters = {
          contactMe          : data.contactMe,
          allowsStatistics   : data.allowsStatistics,
          allowsErrorReports : data.allowsErrorReports,
          agreedToPolicy     : data.agreedToPolicy,
          organisationName   : data.organisationName,
          departmentName     : data.departmentName,
          country            : data.country,
          postalCode         : data.postalCode,
          city               : data.city,
          firstName          : data.firstName,
          lastName           : data.lastName,
          street             : data.street,
          streetNo           : data.streetNo,
          email              : data.email,
          registered         : data.registered
        };

        if (angular.isDefined(data.roles)) {
          parameters.roles = angular.toJson(data.roles);
        }

        return $.param(parameters);
      }
    },
  });

  finalize = $resource('/admin-ng/adopter/registration/finalize/:target', {}, {
    post: {
      params : { target: '' },
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    },
  });

  return {
    get: get.get,
    create : create.post,
    finalize : finalize.post
  };
}]);
