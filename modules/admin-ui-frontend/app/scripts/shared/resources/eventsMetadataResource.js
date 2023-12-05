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
.factory('EventsMetadataResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
  return $resource('/admin-ng/event/events/metadata:ext', {}, {
    get: {
      method: 'POST',
      params: { ext: '.json' },
      responseType: 'json',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformResponse: function (response) {
        var data = response;
        if (angular.isDefined(data) && data != null && data.length > 0) {
          data = JSON.parse(data);
          JsHelper.replaceBooleanStrings(data);
        }
        return { results: data};
      },
      transformRequest: function (data) {
        return $.param({
          eventIds : JSON.stringify(data)
        });
      }
    },
    save: {
      method: 'PUT',
      params: { ext: '' },
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformRequest: function (data) {
        var metadataWrapped = [{flavor: 'dublincore/episode',title:'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
          fields:data.metadata}];
        return $.param({
          eventIds : JSON.stringify(data.eventIds),
          metadata: JSON.stringify(metadataWrapped)
        });
      },
      transformResponse: function(data) {
        if (angular.isDefined(data) && data != null && data.length > 0) { //empty responses are possible
          data = JSON.parse(data);
        }
        return { errors: data };
      }
    }
  });
}]);
