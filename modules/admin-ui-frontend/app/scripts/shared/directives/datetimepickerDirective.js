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

angular.module('adminNg.directives')
.directive('datetimepicker', ['Language', function (Language) {

  function getCurrentLanguageCode() {
    var lc = Language.getLanguageCode() || 'en';
    lc = lc.replace(/_.*/, ''); // remove long locale, as the datepicker does not support this
    return lc;
  }


  return {
    // Enforce the angularJS default of restricting the directive to // attributes only
    restrict: 'A',
    // Always use along with an ng-model
    require: '?ngModel',
    scope: {
      // This method needs to be defined and
      // passed in to the directive from the view controller
      select: '&' // Bind the select function we refer to the
      // right scope
    },
    link: function (scope, element, attrs, ngModel) {
      var dateValue;

      if (scope.params) {
        dateValue = new Date(scope.params);
      } else if (scope.$parent && scope.$parent.params && scope.$parent.params.value) {
        dateValue = new Date(scope.$parent.params.value);
      }

      if (ngModel) {

        var updateModel = function (date) {
          scope.$apply(function () {
            // Call the internal AngularJS helper to
            // update the two-way binding
            ngModel.$setViewValue(date.toISOString());
          });
        };


        var optionsObj = {};
        optionsObj.timeInput = true;
        optionsObj.showButtonPanel = true;
        optionsObj.onClose = function () {
          var newDate = element.datetimepicker('getDate');
          setTimeout(function(){
            updateModel(newDate);
            if (scope.select) {
              scope.$apply(function () {
                scope.select({date: newDate});
              });
            }
          });
        };

        var lc = getCurrentLanguageCode();
        $.datepicker.setDefaults($.datepicker.regional[lc]);
        $.timepicker.setDefaults($.timepicker.regional[lc === 'en' ? '' : lc]);

        element.datetimepicker(optionsObj);

        if (dateValue) {
          element.datetimepicker('setDate', dateValue);
          ngModel.$setViewValue(dateValue.toISOString());
        }
      }

      scope.$on('$destroy', function () {
        try {
          element.datetimepicker('destroy');
        } catch (e) {}
      });
    }
  };
}]);
