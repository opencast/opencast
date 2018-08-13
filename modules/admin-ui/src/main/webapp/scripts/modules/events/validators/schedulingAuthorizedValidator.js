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
 * @name adminNg.modules.events.validators.schedulingAuthorized
 * @description
 * Checks if the current user is authorized to change scheduling of current event.
 *
 */
angular.module('adminNg.directives')
.directive('schedulingAuthorized', ['Notifications', 'SchedulingHelperService', function (Notifications, SchedulingHelperService) {
    var link = function (scope, elm, attrs, ctrl) {
        scope, elm, attrs, ctrl, Notifications;
        ctrl.$validators.schedulingAuthorized = function (modelValue, viewValue) {
            if (viewValue) {
                if (angular.isDefined(attrs.schedulingAuthorized)) {
                    var event = JSON.parse(attrs.schedulingAuthorized);
                    return event.source !== "SCHEDULE" || SchedulingHelperService.hasAgentAccess(event.agent_id);
                }
                else {
                    return false;
                }
            }
            else {
                return true;
            }
        };
    };

    return {
        require: 'ngModel',
        link: link
    };
}]);
