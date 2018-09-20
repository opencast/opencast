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
.factory('RecipientsResource', ['$resource', function ($resource) {
  return $resource('/admin-ng/:category/:resource/recipients');
}])
.factory('EmailPreviewResource', ['$resource', function ($resource) {
  return $resource('/email/preview/:templateId', {}, {
    save: {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {
        if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
          return data;
        }

        var request = {};

        request.eventIds = data.recipients.items.recordings
                    .map(function (item) { return item.id; }).join(',');
        request.personIds    = data.recipients.items.recipients
                    .map(function (item) { return item.id; }).join(',');
        request.signature    = data.message.include_signature ? true : false;
        request.body         = data.message.message;

        return $.param(request);
      }
    }
  });
}])
.factory('EmailResource', ['$resource', function ($resource) {
  return $resource('/email/send/:templateId', {}, {
    save: {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {
        if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
          return data;
        }

        var request = {};

        request.eventIds = data.recipients.items.recordings
                    .map(function (item) { return item.id; }).join(',');
        request.personIds    = data.recipients.items.recipients
                    .map(function (item) { return item.id; }).join(',');
        request.signature    = data.message.include_signature ? true : false;
        request.subject      = data.message.subject;
        request.body         = data.message.message;
        request.store        = data.recipients.audit_trail ? true : false;

        return $.param(request);
      }
    }
  });
}]);
