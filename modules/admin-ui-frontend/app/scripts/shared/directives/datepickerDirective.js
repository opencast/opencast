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
.directive('datepicker', ['Language', function (Language) {

  function getCurrentLanguageCode() {
    var lc = Language.getLanguageCode();
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

      var defaultDate;

      if (scope.params) {
        defaultDate = new Date(scope.params);
      } else if (scope.$parent && scope.$parent.params && scope.$parent.params.value) {
        defaultDate = new Date(scope.$parent.params.value.replace(/(\d{2})-(\d{2})-(\d{4})/, '$2/$1/$3'));
      }


      if (ngModel) {

        var updateModel = function (dateTxt) {
          scope.$apply(function () {
            // Call the internal AngularJS helper to
            // update the two-way binding
            ngModel.$setViewValue(dateTxt);
          });
        };


        var optionsObj = {};
        optionsObj.defaultDate = defaultDate;
        optionsObj.dateFormat = 'yy-mm-dd';
        optionsObj.onSelect = function (dateTxt, picker) {
          setTimeout(function(){
            var year = picker.selectedYear,
                month = picker.selectedMonth + 1,
                day = picker.selectedDay;

            updateModel(year + '-' + ('0' + month).slice(-2) + '-' + ('0' + day).slice(-2));
            if (scope.select) {
              scope.$apply(function () {
                scope.select({date: dateTxt});
              });
            }
          });

          $.datepicker._hideDatepicker();
        };

        $.datepicker.setDefaults($.datepicker.regional[getCurrentLanguageCode()]);

        element.datepicker(optionsObj);
      }

      scope.$on('$destroy', function () {
        try {
          element.datepicker('destroy');
        } catch (e) { }
      });
    }
  };
}]);
