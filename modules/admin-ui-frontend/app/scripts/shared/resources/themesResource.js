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

    /**
     *
     */
    .factory('ThemesResource', ['$resource', 'Language', 'ResourceHelper',
      function ($resource, Language, ResourceHelper) {

        var themesConverter = function (r) {
          var row = {};
          row.id = r.id;
          row.name = r.name;
          row.description = r.description;
          row.default = r.default;
          row.creator = r.creator;
          row.creation_date = Language.formatDate('short', r.creationDate);
          row.usage = r.usage;
          row.type = 'THEME';
          return row;
        };

        return $resource('/admin-ng/themes/themes.json', {}, {

          /**
                 * Returns a list of themes mainly used by the table implementation.
                 */
          query: {method: 'GET', isArray: false, cancellable: true, transformResponse: function (json) {
            return ResourceHelper.parseResponse(json, themesConverter);
          }}
        });
      }])
    /**
     * Supports queries for series assigned to a given theme.
     */
    .factory('ThemeUsageResource', ['$resource',
      function ($resource) {
        return $resource('/admin-ng/themes/:themeId/usage.json', {});
      }])

    /**
     * Supports CRUD operations on themes.
     */
    .factory('ThemeResource', ['$resource',  'Language', 'ResourceHelper',
      function ($resource) {
        return $resource('/admin-ng/themes/:id:format', {}, {

          /**
                 * Returns a list of themes mainly used by the table implementation.
                 */
          get: {method: 'GET', isArray: false, transformResponse: function (json) {
            var data = JSON.parse(json), result = {};
            result.general = {
              'name': data.name,
              'description': data.description,
              'default': data.default
            };
            result.bumper = {
              'active': data.bumperActive,
              'file': {
                id: data.bumperFile,
                name: data.bumperFileName,
                url: data.bumperFileUrl
              }
            };
            result.trailer = {
              'active': data.trailerActive,
              'file': {
                id: data.trailerFile,
                name: data.trailerFileName,
                url: data.trailerFileUrl
              }
            };
            result.titleslide = {
              active: data.titleSlideActive,
              file: {
                id: data.titleSlideBackground,
                name: data.titleSlideBackgroundName,
                url: data.titleSlideBackgroundUrl
              },
              mode: data.titleSlideBackground ? 'upload' : 'extract'
            },
            result.license = {
              active: data.licenseSlideActive,
              backgroundImageActive: angular.isDefined(data.licenseSlideBackground),
              description: data.licenseSlideDescription,
              file: {
                id: data.licenseSlideBackground,
                name: data.licenseSlideBackgroundName,
                url: data.licenseSlideBackgroundUrl
              }
            };
            result.watermark = {
              active: data.watermarkActive,
              file: {
                id: data.watermarkFile,
                name: data.watermarkFileName,
                url: data.watermarkFileUrl
              },
              position: data.watermarkPosition
            };
            return result;
          }},
          update: {
            method: 'PUT',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            transformRequest: function (data) {
              var d = {};
              if (angular.isUndefined(data)) {
                return data;
              }
              // Temporarily commented out because the backend is not yet ready
              //d.default = data.general.default.toString();
              d.description = data.general.description;
              d.name = data.general.name;

              d.bumperActive = data.bumper.active.toString();
              if (data.bumper.active) {
                d.bumperFile = data.bumper.file.id.toString();
              }

              d.trailerActive = data.trailer.active.toString();
              if (data.trailer.active) {
                d.trailerFile = data.trailer.file.id.toString();
              }

              d.titleSlideActive = data.titleslide.active.toString();
              if (data.titleslide.active) {
                if ('upload' === data.titleslide.mode) {
                  d.titleSlideBackground = data.titleslide.file.id.toString();
                }
              }

              d.licenseSlideActive = data.license.active.toString();
              if (data.license.active) {
                d.licenseSlideDescription = data.license.description;
                if (data.license.backgroundImage) {
                  d.licenseSlideBackground = data.license.file.id.toString();
                }
              }
              d.watermarkActive = data.watermark.active.toString();
              if (data.watermark.active) {
                d.watermarkFile = data.watermark.file.id.toString();
                d.watermarkPosition = data.watermark.position;
              }

              return $.param(d);

            }
          }
        });
      }]);



