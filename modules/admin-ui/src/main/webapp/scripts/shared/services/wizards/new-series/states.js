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
.factory('NewSeriesStates', ['NewSeriesMetadata', 'NewSeriesMetadataExtended', 'NewSeriesAccess', 'NewSeriesTheme',
  'NewSeriesSummary',
  function (NewSeriesMetadata, NewSeriesMetadataExtended, NewSeriesAccess, NewSeriesTheme, NewSeriesSummary) {
    return {
      get: function () {
        return [{
          translation: 'EVENTS.SERIES.NEW.METADATA.CAPTION',
          name: 'metadata',
          stateController: NewSeriesMetadata
        }, {
          translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
          name: 'metadata-extended',
          stateController: NewSeriesMetadataExtended
        }, {
          translation: 'EVENTS.SERIES.NEW.ACCESS.CAPTION',
          name: 'access',
          stateController: NewSeriesAccess
        }, {
          translation: 'EVENTS.SERIES.NEW.THEME.CAPTION',
          name: 'theme',
          stateController: NewSeriesTheme
        },
        {
          translation: 'EVENTS.SERIES.NEW.SUMMARY.CAPTION',
          name: 'summary',
          stateController: NewSeriesSummary
        }];
      }
    };
  }]);
