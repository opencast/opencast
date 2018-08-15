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
.factory('SchedulingHelperService', ['AuthService', function (AuthService) {
    var SchedulingHelperService = function () {

        var me = this;

        this.parseDate = function(dateStr, hour, minute) {
            var dateArray = dateStr.split("-");
            return new Date(dateArray[0], parseInt(dateArray[1]) - 1, dateArray[2], hour, minute);
        }

        this.getUpdatedEndDateSingle = function(start, end) {
            var startDate = me.parseDate(start.date, start.hour, start.minute);
            if (end.hour < start.hour) {
                startDate.setHours(24);
            }
            return moment(startDate).format('YYYY-MM-DD');
        };

        this.applyTemporalValueChange = function(temporalValues, change, single){
            var startMins = temporalValues.start.hour * 60 + temporalValues.start.minute;
            switch(change) {
                case "duration":
                    // Update end time
                    var durationMins = temporalValues.duration.hour * 60 + temporalValues.duration.minute;
                    temporalValues.end.hour = (Math.floor((startMins + durationMins) / 60)) % 24;
                    temporalValues.end.minute = (startMins + durationMins) % 60;
                    break;
                case "start":
                case "end":
                    // Update duration
                    var endMins = temporalValues.end.hour * 60 + temporalValues.end.minute;
                    if (endMins < startMins) endMins += 24 * 60; // end is on the next day
                    temporalValues.duration.hour = Math.floor((endMins - startMins) / 60);
                    temporalValues.duration.minute = (endMins - startMins) % 60;
                    break;
                default:
                    return;
            }
            if (single) {
                temporalValues.end.date = me.getUpdatedEndDateSingle(temporalValues.start, temporalValues.end);
            }
        };

        this.hasAgentAccess = function(agentId) {
            if (AuthService.isOrganizationAdmin()) return true;
            var role = "ROLE_CAPTURE_AGENT_" + agentId.replace(/[^a-zA-Z0-9_]/g, "").toUpperCase();
            return AuthService.userIsAuthorizedAs(role);
        };
    };
    return new SchedulingHelperService();
}]);
