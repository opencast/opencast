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
.directive('adminNgNotifications', ['Notifications', function (Notifications) {
  return {
    restrict: 'A',
    templateUrl: 'shared/partials/notifications.html',
    replace: true,
    scope: {
      context: '@',
    },
    link: function (scope) {
      var updateNotifications = function (context) {
        if (angular.isUndefined(scope.context)) {
          scope.context = 'global';
        }

        if (scope.context === context ) {
          scope.notifications = Notifications.get(scope.context);
        }
      };

      scope.notifications = Notifications.get(scope.context);

      Notifications.$on('added', function (event, context) {
        updateNotifications(context);
      });

      Notifications.$on('changed', function (event, context) {
        updateNotifications(context);
      });

      Notifications.$on('deleted', function (event, context) {
        updateNotifications(context);
      });
    }
  };
}]);
