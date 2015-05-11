/**
* Copyright 2009-2013 The Regents of the University of California
* Licensed under the Educational Community License, Version 2.0
* (the "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.osedu.org/licenses/ECL-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an "AS IS"
* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*
*/
'use strict';

angular.module('adminNg').config(['$provide', '$httpProvider', function ($provide, $httpProvider) {
  
  // Intercept http calls.
  $provide.factory('HttpInterceptor', ['$q', 'Notifications', function ($q, Notifications) {
    var unresponsiveNotifications;


    return {

      response: function (response) {
        if (angular.isDefined(unresponsiveNotifications)) {
          Notifications.remove(unresponsiveNotifications);
          unresponsiveNotifications = undefined;
        }

        return response;
      },
 
      responseError: function (rejection) {
        if (rejection.status === 0) {
            if (angular.isDefined(unresponsiveNotifications)) {
              Notifications.remove(unresponsiveNotifications);
            }
            unresponsiveNotifications = Notifications.add('error', 'SERVER_UNRESPONSIVE', 'global', -1);
        } else if (rejection.status === 419) {
          // Try to access index.html again --> will redirect to the login page
          location.reload(true);
        }

        return $q.reject(rejection);
      }
    };
  }]);
 
  // Add the interceptor to the $httpProvider.
  $httpProvider.interceptors.push('HttpInterceptor');
 
}]);