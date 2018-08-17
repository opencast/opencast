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

angular.module('adminNg.services')
.provider('Notifications', function () {
    var notifications = {},
        keyList = {},
        uniqueId = 0;

    this.$get = ['$rootScope', '$injector', function ($rootScope, $injector) {
        var AuthService;

        // notification durations for different log level in seconds
        var notificationDurationError = -1,
            notificationDurationSuccess = 5,
            notificationDurationWarning = 5;

        var scope = $rootScope.$new(),
            initContext = function (context) {
                if (angular.isDefined(keyList[context])) {
                    return notifications[context];
                }

                // initialize the arrays the first time
                keyList[context] = [];
                notifications[context] = {};

                return notifications[context];
            };

        scope.get = function (context) {
            var ADMIN_NOTIFICATION_DURATION_ERROR = 'admin.notification.duration.error',
                ADMIN_NOTIFICATION_DURATION_SUCCESS = 'admin.notification.duration.success',
                ADMIN_NOTIFICATION_DURATION_WARNING = 'admin.notification.duration.warning';

            // We bind to AuthService here to prevent a circular dependency to $http
            if (!AuthService) { AuthService = $injector.get('AuthService'); }

            if (AuthService) {
                AuthService.getUser().$promise.then(function(user) {
                    if (angular.isDefined(user.org.properties[ADMIN_NOTIFICATION_DURATION_ERROR])) {
                        notificationDurationError = user.org.properties[ADMIN_NOTIFICATION_DURATION_ERROR];
                    }
                    if (angular.isDefined(user.org.properties[ADMIN_NOTIFICATION_DURATION_SUCCESS])) {
                        notificationDurationSuccess = user.org.properties[ADMIN_NOTIFICATION_DURATION_SUCCESS];
                    }
                    if (angular.isDefined(user.org.properties[ADMIN_NOTIFICATION_DURATION_WARNING])) {
                        notificationDurationWarning = user.org.properties[ADMIN_NOTIFICATION_DURATION_WARNING];
                    }
                });
            }

            if (!context) {
                context = 'global';
            }
            return notifications[context] || initContext(context);
        };

        scope.remove = function (id, context) {
            var key;
            if (!context) {
                context = 'global';
            }

            if (notifications[context] && notifications[context][id]) {
                // remove from key list
                key = notifications[context][id].key;
                keyList[context].splice(keyList[context].indexOf(key), 1);

                notifications[context][id] = undefined;
                delete notifications[context][id];
                scope.$emit('deleted', context);
            }
        };

        scope.removeAll = function(context) {

            if (!context) {
                context = 'global';
            }

            delete keyList[context]
            delete notifications[context]

            scope.$emit('deleted', context);
        }

        scope.addWithParams = function (type, key, messageParams, context, duration) {
            scope.add(type, key, context, duration, messageParams);
        };

        scope.add = function (type, key, context, duration, messageParams) {
            if (angular.isUndefined(duration)) {
                // fall back to defaults
                switch (type) {
                    case 'error':
                        duration = notificationDurationError;
                        break;
                    case 'success':
                        duration = notificationDurationSuccess;
                        break;
                    case 'warning':
                        duration = notificationDurationWarning;
                        break;
                }
                // default durations are in seconds. duration needs to be in milliseconds
                if (duration > 0) duration *= 1000;
            }

            if (!context) {
                context = 'global';
            }

            if (!messageParams) {
                messageParams = {};
            }

            initContext(context);

            if(keyList[context].indexOf(key) < 0) {
                // only add notification if not yet existent

                // add key to an array
                keyList[context].push(key);

                notifications[context][++uniqueId] = {
                    type       : type,
                    key        : key,
                    message    : 'NOTIFICATIONS.' + key,
                    parameters : messageParams,
                    duration   : duration,
                    id         : uniqueId,
                    hidden     : false
                };

                scope.$emit('added', context);
            } else {
              var notification = _.find(notifications.global, function(a) {return a.key === key});
              if(notification) {
                  notification.hidden = false;
              }
            }
            return uniqueId;
        };

        return scope;
    }];
});
