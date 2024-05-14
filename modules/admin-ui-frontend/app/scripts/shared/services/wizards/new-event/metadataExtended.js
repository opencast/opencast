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
.factory('NewEventMetadataExtended', ['NewEventMetadataResource', 'EventMetadataResource',
  function (NewEventMetadataResource, EventMetadataResource) {
    var MetadataExtended = function () {
      var me = this, mainMetadataName = 'dublincore/episode', i;
      this.ud = {};
      this.requiredMetadata = {};
      this.copyEventId = undefined;

      me.isMetadataExtendedState = true;

      this.updateRequiredMetadata = function(fieldId, value) {
        if (angular.isDefined(value) && value.length > 0) {
          me.requiredMetadata[fieldId] = true;
        } else {
          me.requiredMetadata[fieldId] = false;
        }
      };

      // Set Id of the event used as a template for this new one
      this.setCopyEventId = function(copyEventId) {
        me.copyEventId = copyEventId;
      };

      // Fetch additional information if we've got a template event
      this.findRequiredMetadata = function (data) {
        if(me.copyEventId === undefined) {
          me.postProcessMetadata(data);
        } else {
          EventMetadataResource.get({ id: me.copyEventId }, function (copyMetadata) {
            var copyExtendedMetadata = {};
            angular.forEach(copyMetadata.entries, function (catalog, index) {
              if (catalog.flavor !== mainMetadataName) {
                copyExtendedMetadata[catalog.flavor] = catalog;
              }
            });

            me.postProcessMetadata(copyExtendedMetadata);
          });
        }
      };

      // As soon as the required metadata fields arrive from the backend,
      // we check which are mandatory.
      // This information will be needed in order to tell if we can move
      // on to the next page of the wizard.
      this.postProcessMetadata = function (data) {
        var fields = [], chunk;
        for (chunk in data) {
          if (Object.prototype.hasOwnProperty.call(data, chunk)) {
            // extended metadata is every object in the returned data which
            // does not start with a dollar sign and which isn't dublincore/episode
            if (chunk !== mainMetadataName && chunk.charAt(0) !== '$') {
              me.ud[chunk] = {fields: data[chunk].fields};
              me.ud[chunk].flavor = data[chunk].flavor;
              me.ud[chunk].title = data[chunk].title;
              fields = fields.concat(data[chunk].fields);
            }
          }
        }
        // we go for the extended metadata here
        if (fields.length > 0) {
          for (i = 0; i < fields.length; i++) {
            // just hooking the tab index up here, as this is already running through all elements
            fields[i].tabindex = i + 1;
            if (fields[i].required) {
              me.updateRequiredMetadata(fields[i].id, fields[i].value);
              if (fields[i].type === 'boolean') {
                // set all boolean fields to false by default
                fields[i].value = false;
                // remark: since booleans are always present, we
                // do not add this property to the required fields.
              }
            }
          }
          me.visible = true;
        }
        else {
          me.visible = false;
        }
      };

      this.metadata = NewEventMetadataResource.get(this.findRequiredMetadata);

      // Checks if the current state of this wizard is valid and we are
      // ready to move on.
      this.isValid = function () {
        var result = true;
        //FIXME: The angular validation should rather be used,
        // unfortunately it didn't work in this context.
        angular.forEach(me.requiredMetadata, function (item) {
          if (item === false) {
            result = false;
          }
        });
        return result;
      };

      this.save = function (scope) {
        //FIXME: This should be nicer,
        //rather propagate the id and values instead of looking for them in the parent scope.
        var field = scope.$parent.params,
            target = scope.$parent.target;

        if (field.collection) {
          if (angular.isArray(field.value)) {
            field.presentableValue = field.value;
          } else {
            field.presentableValue = field.collection[field.value];
          }
        } else {
          field.presentableValue = field.value;
        }

        if (angular.isDefined(me.requiredMetadata[field.id])) {
          me.updateRequiredMetadata(field.id, field.value);
        }

        var i = me.ud[target].fields.findIndex(function (f) {
          return f.id === field.id;
        });
        me.ud[target].fields[i] = field;
      };

      this.reset = function () {
        me.ud = {};
        me.metadata = NewEventMetadataResource.get(me.findRequiredMetadata);
      };

      this.getFiledCatalogs = function () {
        var catalogs = [];

        angular.forEach(me.ud, function(catalog) {
          var empty = true;
          angular.forEach(catalog.fields, function (field) {
            if (angular.isDefined(field.presentableValue) && field.presentableValue !== '') {
              empty = false;
            }
          });

          if (!empty) {
            catalogs.push(catalog);
          }
        });

        return catalogs;
      };

      this.getUserEntries = function () {
        angular.forEach(me.ud, function(catalog) {
          catalog.empty = true;
          angular.forEach(catalog.fields, function (field) {
            if (angular.isDefined(field.presentableValue) && field.presentableValue !== '') {
              catalog.empty = false;
            }
          });
        });

        return me.ud;
      };
    };

    return new MetadataExtended();
  }]);
