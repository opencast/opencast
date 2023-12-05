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

angular.module('adminNg.directives')
.directive('datetimepicker', ['Language', function (Language) {

  function getCurrentLanguageCode() {
    var lc = Language.getLanguageCode() || 'en';
    lc = lc.replace(/_.*/, ''); // remove long locale, as the datepicker does not support this
    return lc;
  }

  // When you edit the time in the input field directly,
  // instead of using any of the picker's means,
  // the `timepicker` addon syncs that change with it's internal state,
  // to reflect it in its controls.
  // As part of this, it also sets the selection in one of them.
  // This is weird and probably a bug, but this library is also all but dead.
  // Anyway, in Safari this does weird stuff to the focus,
  // making it impossible to enter more than one keystroke most of the time.
  //
  // The workaround is similarly crazy:
  // The input field that has is selection updated
  // is actually never shown in our use of the time picker.
  // By inspecting the code I found the place where it is updated,
  // and I also checked that its state is not used for anything else.
  // (It isn't.)
  // So since this is JavaScript, we can just monkey-patch the appropriate function
  // and get rid of the element before calling through to the original.
  // In a very lucky turn of events, said function actually defensively checks
  // whether or not the element is there, instead of horribly breaking
  // like you would expect from most jQuery-heavy code. 👀😮‍💨
  //
  // If you are wondering whether one could remove the element earlier,
  // like immediately after construction instead of every time the time value changes:
  // You can't, because the internal DOM is rebuilt every time that happens.
  //
  // Of course this will break when the `timepicker` is ever updated
  // which—see above—will obviously never happen. 🤷‍♀️
  var baseTimepickerOnTimeChange = $.timepicker._onTimeChange;
  $.timepicker.constructor.prototype._onTimeChange = function () {
    delete this.$timeObj;
    return baseTimepickerOnTimeChange.apply(this, arguments);
  };

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


        var optionsObj = {
          controlType: 'select',
          showMillisec: false,
          showMicrosec: false,
          showTimezone: false,
          oneLine: true,
          timeFormat: 'HH:mm'
        };
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
