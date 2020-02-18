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
 * @name adminNg.modules.events.validators.taskStartableValidator
 * @description Checks if the chosen selection is valid. We would have expected ng-required to suffice, but with this
 *              chosen component, it doesn't.
 *
 */
angular.module('adminNg.directives')
.directive('notEmptySelection', ['Notifications', function (Notifications) {
  var link = function (scope, elm, attrs, ctrl) {
    scope, elm, attrs, ctrl, Notifications;
    var $scope = scope;
    ctrl.$validators.notEmptySelection = function () {
      var workflow = $scope.processing.ud.workflow;
      if (angular.isDefined(workflow)) {
        if (angular.isObject(workflow.selection)) {
          return angular.isDefined(workflow.selection.id) && workflow.selection.id.length > 0;
        }
        else {
          return false;
        }
      }
      else {
        return false;
      }
    };
  };

  return {
    require: 'ngModel',
    link: link
  };
}]);
