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

angular.module('adminNg.services')
.factory('NewEventMetadata', ['NewEventMetadataResource', 'EventMetadataResource',
  function (NewEventMetadataResource, EventMetadataResource) {
    var Metadata = function () {
      var me = this, mainMetadataName = 'dublincore/episode', i;
      me.ud = {};
      me.isMetadataState = true;
      me.copyEventId = undefined;

      this.requiredMetadata = {};

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
          me.setupRequiredMetadata(data);
        } else {
          EventMetadataResource.get({ id: me.copyEventId }, function (copyMetadata) {
            var copyMainMetadata;
            angular.forEach(copyMetadata.entries, function (catalog) {
              if (catalog.flavor === mainMetadataName) {
                copyMainMetadata = catalog;
              }
            });

            // FIXME: Instead of assigning the fields from copyMetadata, it
            //  would be cleaner to just set the values in default data.
            //  But for whatever reason single value fields don't initially display
            //  in the Admin UI when you do that (the values are there, they just
            //  are not displayed).
            // Filter superfluous fields (e.g. UID)
            var fieldsFiltered = copyMainMetadata.fields.filter((el) => {
              return data[mainMetadataName].fields.some((f) => {
                return f.id === el.id;
              });
            });
            // Set certain properties to their default value
            angular.forEach(fieldsFiltered, function (fieldFiltered, i) {
              // var fieldFiltered = fieldsFiltered[i];
              var fieldDefault = data[mainMetadataName].fields.find(field => {
                return field.id === fieldFiltered.id;
              });
              fieldsFiltered[i].readOnly = fieldDefault.readOnly;
            });
            // Overwrite defaults with the new fields
            data[mainMetadataName].fields = fieldsFiltered;

            me.setupRequiredMetadata(data);
          });
        }
      };

      // As soon as the required metadata fields arrive from the backend,
      // we check which are mandatory.
      // This information will be needed in ordert to tell if we can move
      // on to the next page of the wizard.
      this.setupRequiredMetadata = function(data) {
        var mainData = data[mainMetadataName];
        // we go for the regular dublincore metadata here
        me.ud[mainMetadataName] = mainData;
        if (mainData && mainData.fields) {
          for (i = 0; i < mainData.fields.length; i++) {
            var field = mainData.fields[i];
            // preserve default value, if set
            if (Object.prototype.hasOwnProperty.call(field, 'value') && field.value) {
              field.presentableValue = me.extractPresentableValue(field);
              me.ud[mainMetadataName].fields[i] = field;
            }
            // just hooking the tab index up here, as this is already running through all elements
            field.tabindex = i + 1;
            if (field.required) {
              me.updateRequiredMetadata(field.id, field.value);
              if (field.type === 'boolean') {
                // set all boolean fields to false by default
                field.value = false;
                me.requiredMetadata[field.id] = true;
              }
            }
          }
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

      this.extractPresentableValue = function (field) {
        var presentableValue = '';
        if (field.value !== undefined && field.value !== '' && field.value !== null) {
          if (angular.isArray(field.value)) {
            presentableValue = field.value.join(', ');
          } else if (field.collection) {
            // We need to lookup the presentable value in the collection
            if (Object.prototype.hasOwnProperty.call(field.collection, field.value)) {
              presentableValue = field.collection[field.value];
            } else {
              // This should work in older browsers, albeit looking clumsy
              var matchingKey = Object.keys(field.collection)
                .filter(function(key) {return field.collection[key] === field.value;})[0];
              presentableValue = field.type === 'ordered_text'
                ? JSON.parse(matchingKey)['label']
                : matchingKey;
            }
          } else {
            presentableValue = field.value;
          }
        }
        return presentableValue;
      };

      this.save = function (scope) {
        //FIXME: This should be nicer, rather propagate the id and values instead of looking for them
        // in the parent scope.
        var field = scope.$parent.params;
        field.presentableValue = me.extractPresentableValue(field);

        if (angular.isDefined(me.requiredMetadata[field.id])) {
          me.updateRequiredMetadata(field.id, field.value);
        }

        me.ud[mainMetadataName].fields[field.tabindex - 1] = field;
      };

      this.reset = function () {
        me.ud = {};
        me.metadata = NewEventMetadataResource.get(this.findRequiredMetadata);
      };

      this.getUserEntries = function () {
        if (angular.isDefined(me.ud[mainMetadataName])) {
          return me.ud[mainMetadataName].fields;
        } else {
          return {};
        }
      };
    };

    return new Metadata();
  }]);
