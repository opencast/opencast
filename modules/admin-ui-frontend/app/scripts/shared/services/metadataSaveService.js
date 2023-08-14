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

angular.module('adminNg.services')
.service('MetadataSaveService', ['EventMetadataResource', 'Notifications',
  function (EventMetadataResource, Notifications) {

    return {
      save: function(catalogs, commonMetadataCatalog, resourceId){
        var catalogsWithUnsavedChanges = catalogs.filter(function(catalog) {
          return catalog.fields.some(function(field) {
            return field.dirty === true;
          });
        });

        var promisesToReturn = [];

        catalogsWithUnsavedChanges.forEach(function(catalog) {
          // don't send collections
          catalog.fields.forEach(function(field) {
            if (Object.prototype.hasOwnProperty.call(field, 'collection')) {
              field.collection = [];
            }
          });

          var promise = EventMetadataResource.save({ id: resourceId }, catalog,  function () {
            var notificationContext = catalog === commonMetadataCatalog ? 'events-metadata-common'
              : 'events-metadata-extended';
            Notifications.add('info', 'SAVED_METADATA', notificationContext, 1200);

            // Unmark entries
            angular.forEach(catalog.fields, function (entry) {
              entry.dirty = false;
              // new original value
              if (entry.value instanceof Array) {
                entry.oldValue = entry.value.slice(0);
              } else {
                entry.oldValue = entry.value;
              }
            });
          }).$promise;
          promisesToReturn.push(promise);
        });

        return promisesToReturn;
      }
    };
  }
]);
