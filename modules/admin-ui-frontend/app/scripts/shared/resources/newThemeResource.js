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
    .factory('NewThemeResource', ['$resource', function ($resource) {

      return $resource('/admin-ng/themes', {}, {
        save: {
          method: 'POST',
          headers: {'Content-Type': 'application/x-www-form-urlencoded'},

          transformRequest: function (data) {
            var d = {};
            if (angular.isUndefined(data)) {
              return data;
            }
            // Temporarily commented out - the backend is not yet ready.
            //d.default = data.general.default;
            d.description = data.general.description;
            d.name = data.general.name;

            d.bumperActive = data.bumper.active;
            if (data.bumper.active) {
              d.bumperFile = data.bumper.file.id;
            }

            d.trailerActive = data.trailer.active;
            if (data.trailer.active) {
              d.trailerFile = data.trailer.file.id;
            }

            d.titleSlideActive = data.titleslide.active;
            if (data.titleslide.active) {

              if ('upload' === data.titleslide.mode) {
                d.titleSlideBackground = data.titleslide.file.id;
              }
            }

            d.licenseSlideActive = data.license.active;
            if (data.license.active) {
              d.licenseSlideDescription = data.license.description;
              if (data.license.backgroundImage) {
                d.licenseSlideBackground = data.license.file.id;
              }
            }
            d.watermarkActive = data.watermark.active;
            if (data.watermark.active) {
              d.watermarkFile = data.watermark.file.id;
              d.watermarkPosition = data.watermark.position;
            }

            return $.param(d);
          }
        }
      }
      );
    }
    ]);
