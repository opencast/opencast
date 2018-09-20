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
.factory('EmailTemplatesResource', ['$resource', 'Language', 'ResourceHelper',
  function ($resource, Language, ResourceHelper) {
    return $resource('/email/templates.json', {}, {
      query: {method: 'GET', isArray: false, transformResponse: function (json) {
        return ResourceHelper.parseResponse(json, function (r) {
          var row = {};
          row.id = r.id;
          row.name = r.name;
          row.created = Language.formatDate('short', r.creationDate);
          row.creator = r.creator.username;
          row.type = 'EMAIL';
          return row;
        });
      }}
    });
  }])
.factory('EmailTemplateResource', ['$resource',
  function ($resource) {
    return $resource('/email/template/:id', {}, {
      get: {
        method: 'GET',
        transformResponse: function (data) {
          data = JSON.parse(data);
          return {
            id: data.id,
            name: data.name,
            subject: data.subject,
            message: data.body
          };
        }
      },
      update: {
        method: 'PUT',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        transformRequest: function (data) {
          if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
            return data;
          }

          var request = {};

          request.name    = data.message.name;
          request.type    = 'INVITATION';
          request.subject = data.message.subject;
          request.body    = data.message.message;

          return $.param(request);
        }
      },
      save: {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        transformRequest: function (data) {
          if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
            return data;
          }

          var request = {};

          request.name    = data.message.name;
          request.type    = 'INVITATION';
          request.subject = data.message.subject;
          request.body    = data.message.message;

          return $.param(request);
        }
      }
    });
  }])
.factory('EmailTemplateDemoResource', ['$resource', function ($resource) {
  return $resource('/email/demotemplate');
}])
.factory('EmailVariablesResource', ['$resource', function ($resource) {
  return $resource('/email/variables.json');
}]);
