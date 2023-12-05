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
.factory('NewEventStates', ['NewEventMetadata', 'NewEventMetadataExtended', 'NewEventSource', 'NewEventUploadAsset',
  'NewEventAccess', 'NewEventProcessing', 'NewEventSummary',
  function (NewEventMetadata, NewEventMetadataExtended, NewEventSource, NewEventUploadAsset, NewEventAccess,
    NewEventProcessing, NewEventSummary) {
    return {
      get: function () {
        var states = [
          {
            translation: 'EVENTS.EVENTS.NEW.METADATA.CAPTION',
            name: 'metadata',
            stateController: NewEventMetadata
          },
          {
            translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
            name: 'metadata-extended',
            stateController: NewEventMetadataExtended
          },
          {
            translation: 'EVENTS.EVENTS.NEW.SOURCE.CAPTION',
            name: 'source',
            stateController: NewEventSource
          },
          {
            translation: 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.CAPTION',
            name: 'upload-asset',
            stateController: NewEventUploadAsset
          },
          {
            translation: 'EVENTS.EVENTS.NEW.PROCESSING.CAPTION',
            name: 'processing',
            // This allows us to reuse the processing functionality in schedule task
            stateController: NewEventProcessing.get()
          },
          {
            translation: 'EVENTS.EVENTS.NEW.ACCESS.CAPTION',
            name: 'access',
            stateController: NewEventAccess
          },
          {
            translation: 'EVENTS.EVENTS.NEW.SUMMARY.CAPTION',
            name: 'summary',
            stateController: NewEventSummary
          }
        ];
        return states;
      }
    };
  }]);
