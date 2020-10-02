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

/**
 * @ngdoc directive
 * @name adminNg.modules.configuration.validators.uniquerThemeName
 * @description
 * Makes sure that the model name is unique.
 *
 */
angular.module('adminNg.directives')
.directive('uniqueThemeName', ['ThemesResource', 'Notifications', function (ThemesResource, Notifications) {
  var existingModelValue, link;
  link = function (scope, elm, attrs, ctrl) {
    var existingThemes;
    if (angular.isUndefined(existingThemes)) {
      existingThemes = ThemesResource.get();
    }
    ctrl.$validators.uniqueTheme = function (modelValue, viewValue) {
      var result = true;
      if (!ctrl.$dirty) {
        if (angular.isDefined(ctrl.$modelValue)) {
          existingModelValue = ctrl.$modelValue;
        }
        return true;
      }
      if (ctrl.$isEmpty(viewValue)) {
        Notifications.add('error', 'THEME_NAME_EMPTY', 'new-theme-general');
        // consider empty models to be invalid
        result = false;
      }
      else {
        if (angular.isDefined(existingModelValue)) {
          if (existingModelValue === viewValue) {
            return true; // thats ok
          }
        }
        angular.forEach(existingThemes.results, function (theme) {
          if (theme.name === viewValue) {
            Notifications.add('error', 'THEME_NAME_ALREADY_TAKEN', 'new-theme-general');
            result = false;
          }
        });
      }

      // it is invalid
      return result;
    };
  };
  return {
    require: 'ngModel',
    link: link
  };
}]);
