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
.factory('EventMetadataResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
  var transformResponse = function (data) {
        var metadata = {};
        try {
          metadata = JSON.parse(data);
          angular.forEach(metadata, function (catalog) {
            if (angular.isDefined(catalog.locked)) {
              angular.forEach(catalog.fields, function (field) {
                field.locked = catalog.locked;
                field.readOnly = true;
              });
            }
          });
          JsHelper.replaceBooleanStrings(metadata);
        } catch (e) { }
        return { entries: metadata };
      },
      transformRequest = function (catalog) {
        if (catalog.attributeToSend) {
          var catalogToSave = {
            flavor: catalog.flavor,
            title: catalog.title,
            fields: []
          };

          angular.forEach(catalog.fields, function (entry) {
            if (entry.id === catalog.attributeToSend) {
              catalogToSave.fields.push(entry);
            }
          });
          return $.param({metadata: angular.toJson([catalogToSave])});
        }
        return $.param({metadata: angular.toJson([catalog])});
      };

  return $resource('/admin-ng/event/:id/metadata:ext', { id: '@id' }, {
    get: {
      params: { ext: '.json' },
      method: 'GET',
      transformResponse: transformResponse
    },
    save: {
      method: 'PUT',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      transformResponse: transformResponse,
      transformRequest:  transformRequest
    }
  });
}]);
