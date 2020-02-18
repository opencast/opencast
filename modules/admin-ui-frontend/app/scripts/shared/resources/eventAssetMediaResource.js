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
.factory('EventAssetMediaResource', ['$resource', function ($resource) {
  var transform = function (data) {
    var media = [];
    try {
      media = JSON.parse(data);

      //for every media file item we define the filename
      for(var i = 0; i < media.length; i++){
        var item = media[i];
        var url = item.url;
        item.mediaFileName = url.substring(url.lastIndexOf('/') + 1).split('?')[0];
      }

    } catch (e) { }
    return media;
  };

  return $resource('/admin-ng/event/:id0/asset/media/media.json', {}, {
    get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id'}, transformResponse: transform }
  });
}]);
