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

angular.module('adminNg').config(['$provide', '$httpProvider', function ($provide, $httpProvider) {
  
  // Intercept http calls.
  $provide.factory('HttpInterceptor', ['$q', 'Notifications', function ($q, Notifications) {
    var unresponsiveNotifications,
        addNotification = function (message) {
          if (angular.isDefined(unresponsiveNotifications)) {
              Notifications.remove(unresponsiveNotifications);
          }

          unresponsiveNotifications = Notifications.add('error', message, 'global', -1); 
        };


    return {

      response: function (response) {
        if (angular.isDefined(unresponsiveNotifications)) {
          Notifications.remove(unresponsiveNotifications);
          unresponsiveNotifications = undefined;
        }

        return response;
      },
 
      responseError: function (rejection) {
        switch (rejection.status) {
          case 0:
            addNotification('SERVICE_UNAVAILABLE');
            break;
          case 503:
            addNotification('SERVER_UNRESPONSIVE');
            break;
          case 419:
            // Try to access index.html again --> will redirect to the login page
            location.reload(true);
            break;
        }

        return $q.reject(rejection);
      }
    };
  }]);
 
  // Add the interceptor to the $httpProvider.
  $httpProvider.interceptors.push('HttpInterceptor');
 
}]);
