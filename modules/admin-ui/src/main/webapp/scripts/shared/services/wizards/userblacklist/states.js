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
.factory('NewUserblacklistStates', ['NewUserblacklistItems', 'NewUserblacklistDates', 'NewUserblacklistReason', 'NewUserblacklistSummary',
        function (NewUserblacklistItems, NewUserblacklistDates, NewUserblacklistReason, NewUserblacklistSummary) {
    return {
        get: function () {
            return [{
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.USERS',
                name: 'items',
                stateController: NewUserblacklistItems
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.DATES',
                name: 'dates',
                stateController: NewUserblacklistDates
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.REASON',
                name: 'reason',
                stateController: NewUserblacklistReason
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: NewUserblacklistSummary
            }];
        },
        reset: function () {
            NewUserblacklistItems.reset();
            NewUserblacklistDates.reset();
            NewUserblacklistReason.reset();
        }
    };
}]);
