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
.directive('adminNgNotification', ['Notifications', '$timeout',
  function (Notifications, $timeout) {
    return {
      restrict: 'A',
      templateUrl: 'shared/partials/notification.html',
      replace: true,
      scope: {
        id         : '@',
        type       : '@',
        message    : '@',
        parameters : '@',
        show       : '=',
        hidden     : '=',
        duration   : '@'
      },
      link: function (scope, element) {

        if (angular.isNumber(parseInt(scope.duration)) && parseInt(scope.duration) !== -1) {
          // we fade out the notification if it is not -1 -> therefore -1 means 'stay forever'
          var fadeOutTimer = $timeout(function () {
            element.fadeOut(function () {
              Notifications.remove(scope.id, scope.$parent.context);
            });
          }, scope.duration);

          scope.$on('$destroy', function () {
            $timeout.cancel(fadeOutTimer);
          });
        }
      }
    };
  }]);
