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
.directive('adminNgEditable', ['$filter', 'AuthService', 'ResourcesListResource', function ($filter, AuthService,
  ResourcesListResource) {
  return {
    restrict: 'A',
    templateUrl: 'shared/partials/editable.html',
    transclude: true,
    replace: false,
    scope: {
      params: '=',
      save:   '=',
      target: '@', // can be used to further specify how to save data
      requiredRole: '@'
    },
    link: function (scope, element) {
      scope.mixed = false;
      scope.ordered = false;
      scope.readOnly = false;

      if (scope.params === undefined || scope.params.type === undefined) {
        // Illegal parameters for editable field
        return;
      }

      scope.getDisplayableValue = function () {
        var result;
        if (scope.mode === 'dateValue') {
          result = $filter('localizeDate')(scope.params.value, 'dateTime', 'medium');
        } else if (scope.mode === 'singleSelect') {
          angular.forEach(scope.collection, function(key, value) {
            if (angular.equals(scope.params.value, key)) {
              result = value;
            }
          });
          if ((scope.params.type === 'ordered_text') && angular.isDefined(result)) {
            result = JSON.parse(result)['label'];
          }
        } else if ((scope.mode === 'multiSelect') || (scope.mode === 'multiValue')) {
          result = scope.params.value.join(',');
        } else {
          result = scope.params.value;
        }
        if (scope.params.translatable) {
          result = $filter('translate')(result);
        }
        return result;
      };

      if (scope.params.readOnly) {
        scope.readOnly = true;
      } else {
        if (angular.isDefined(scope.requiredRole)) {
          AuthService.userIsAuthorizedAs(scope.requiredRole, function () { }, function () {
            scope.readOnly = true;
          });
        }
      }

      if (typeof scope.params.collection === 'string') {
        scope.collection = ResourcesListResource.get({ resource: scope.params.collection });
      } else if (typeof scope.params.collection === 'object') {
        scope.collection = scope.params.collection;
      }

      if (scope.params.type === 'boolean') {
        scope.mode = 'booleanValue';
      } else if (scope.params.type === 'date') {
        scope.mode = 'dateValue';
      } else {
        if (scope.params.value instanceof Array) {
          if (scope.collection) {
            if (scope.params.type === 'mixed_text') {
              scope.mixed = true;
            }
            scope.mode = 'multiSelect';
          } else {
            scope.mode = 'multiValue';
          }
        } else {
          if (scope.collection) {
            if (scope.params.type === 'ordered_text') {
              scope.ordered = true;
            }
            scope.mode = 'singleSelect';
          } else {
            scope.mode = 'singleValue';
          }
        }
      }

      if (!scope.readOnly) {
        element.addClass('editable');
      }
    }
  };
}]);
