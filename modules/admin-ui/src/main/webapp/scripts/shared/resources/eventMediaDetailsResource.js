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
.factory('EventMediaDetailsResource', ['$resource', function ($resource) {
  var transform = function (data) {
    var media = {};
    try {
      if (typeof data === 'string') {
        media = JSON.parse(data);
      } else {
        media = data;
      }
      media.video = { previews: [{uri: media.url}] };
      media.url = media.url.split('?')[0];
    } catch (e) {}
    return media;
  };

  return $resource('/admin-ng/event/:id0/asset/media/:id2.json', {}, {
    get: { method: 'GET', isArray: false, transformResponse: transform }
  });
}]);
