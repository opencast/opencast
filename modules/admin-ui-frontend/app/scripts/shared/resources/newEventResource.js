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
.factory('NewEventResource', ['$resource', 'JsHelper', 'ProgressBar', function ($resource, JsHelper, ProgressBar) {

  return $resource('/admin-ng/event/new', {}, {
    save: {
      method: 'POST',

      // By setting ‘Content-Type’: undefined, the browser sets the
      // Content-Type to multipart/form-data for us and fills in the
      // correct boundary. Manually setting ‘Content-Type’:
      // multipart/form-data will fail to fill in the boundary parameter
      // of the request.
      headers: { 'Content-Type': undefined },

      //Track file upload progress
      uploadEventHandlers: {
        progress: function(event) {
          if (event.loaded && event.loaded < event.total) {
            ProgressBar.onUploadFileProgress(event);
          }
        }
      },

      responseType: 'text',

      transformResponse: function () {
        // Update ProgressBar service
        ProgressBar.reset();
      },

      transformRequest: function (data) {

        if (angular.isUndefined(data)) {
          data = [];
          return data;
        }

        // The end point expects a multipart request payload with two fields
        // 1. A form field called 'metadata' containing all userdata
        // 2. A non-form field either called 'presenter', 'presentation' or
        //    'audio' which contains a File object
        // 3. Optional asset config mapping and asset upload files
        var fd = new FormData(),
            source,
            sourceType = data.source.type,
            assetConfig,
            tempAssetList = [],
            flavorList = [];

        // If asset upload files exist, add asset mapping defaults
        // IndexServiceImpl processes the catalog or attachment based on the asset metadata map
        if (data['upload-asset'] && data['upload-asset'].assets) {
          assetConfig = data['upload-asset'].defaults;
        }

        source = {
          type: sourceType
        };

        if (sourceType !== 'UPLOAD') {
          source.metadata = {
            start: JsHelper.toZuluTimeString(data.source[sourceType].start),
            device: data.source[sourceType].device.name,
            inputs: (function (inputs) {
              var result = '';
              angular.forEach(inputs, function (enabled, inputId) {
                if (enabled) {
                  result += inputId + ',';
                }
              });
              // Remove the trailing comma for the last input
              result = result.substring(0, result.length - 1);
              return result;
            })(data.source[sourceType].device.inputMethods)
          };
        }

        if (sourceType === 'SCHEDULE_SINGLE') {
          source.metadata.end = JsHelper.toZuluTimeString(data.source.SCHEDULE_SINGLE.start,
            data.source.SCHEDULE_SINGLE.duration);
          source.metadata.duration = (
            parseInt(data.source.SCHEDULE_SINGLE.duration.hour, 10) * 60 * 60 * 1000 +
                        parseInt(data.source.SCHEDULE_SINGLE.duration.minute, 10) * 60 * 1000
          ).toString();
        }

        if (sourceType === 'SCHEDULE_MULTIPLE') {
          // We need to set it to the end time and day so the last day will be used in the recurrance and the correct
          // end time is used for the rest of the recordings.

          source.metadata.duration = moment.duration(parseInt(data.source.SCHEDULE_MULTIPLE.duration.hour, 10), 'h')
                                           .add(parseInt(data.source.SCHEDULE_MULTIPLE.duration.minute, 10), 'm')
                                           .as('ms') + '';

          source.metadata.end = JsHelper.toZuluTimeString(data.source.SCHEDULE_MULTIPLE.end);

          source.metadata.rrule = (function (src) {
            return JsHelper.assembleRrule(src.SCHEDULE_MULTIPLE);
          })(data.source);
        }

        // Dynamic source config and multiple source per type allowed
        if (sourceType === 'UPLOAD') {
          if (data.source.UPLOAD.tracks) {
            angular.forEach(data.source.UPLOAD.tracks, function(files, name) {
              angular.forEach(files, function (file, index) {
                fd.append(name + '.' + index, file);
              });
            });
          }

          if (data.source.UPLOAD.metadata.start) {
            data.metadata[0].fields.push(data.source.UPLOAD.metadata.start);
          }
        }

        if (assetConfig) {
          angular.forEach(data['upload-asset'].assets, function(files, name) {
            angular.forEach(files, function (file, index) {
              fd.append(name + '.' + index, file);
              tempAssetList.push(name);
            });
          });
          // special case to override creation of search preview when one is uploaded
          assetConfig['options'].forEach(function(dataItem) {
            if (tempAssetList.indexOf(dataItem.id) >= 0) {
              flavorList.push(dataItem.flavorType + '/' + dataItem.flavorSubType);
              if (dataItem.flavorSubType == 'search+preview') {
                data.processing.workflow.selection.configuration['uploadedSearchPreview'] = 'true';
              }
            }
          });
        }

        // set workflow boolean param and flavor list param
        if (flavorList.length > 0) {
          data.processing.workflow.selection.configuration['downloadSourceflavorsExist'] = 'true';
          data.processing.workflow.selection.configuration['download-source-flavors'] = flavorList.join(', ');
        }

        // Remove useless information for the request
        angular.forEach(data.metadata, function (catalog) {
          angular.forEach(catalog.fields, function (field) {
            delete field.collection;
            delete field.label;
            delete field.presentableValue;
            delete field.readOnly;
            delete field.required;
          });
        });

        // Add metadata form field
        fd.append('metadata', JSON.stringify({
          metadata: data.metadata,
          processing: {
            workflow: data.processing.workflow.id,
            configuration: data.processing.workflow.selection.configuration
          },
          access: data.access,
          source: source,
          assets: assetConfig
        }));

        return fd;
      }
    }
  });
}]);

