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
.factory('NewLocationblacklistDates', ['BlacklistCountResource', 'NewLocationblacklistItems', 'JsHelper',
function (BlacklistCountResource, NewLocationblacklistItems, JsHelper) {
    var Dates = function () {
        var me = this;

        this.reset = function () {
            me.ud = { fromDate: null, toDate: null };
        };
        this.reset();

        this.isValid = function () {
            return (me.ud.fromDate && me.ud.toDate && me.ud.fromTime && me.ud.toTime) ? true:false;
        };

        this.updateBlacklistCount = function () {
            if (me.isValid()) {
                var from = JsHelper.toZuluTimeString({
                    date: me.ud.fromDate,
                    hour: me.ud.fromTime.split(':')[0],
                    minute: me.ud.fromTime.split(':')[1]
                }),
                    to = JsHelper.toZuluTimeString({
                    date: me.ud.toDate,
                    hour: me.ud.toTime.split(':')[0],
                    minute: me.ud.toTime.split(':')[1]
                });

                me.blacklistCount = BlacklistCountResource.save({
                    type:          'room',
                    blacklistedId: NewLocationblacklistItems.ud.items[0].id,
                    start:         from,
                    end:           to
                });
            }
        };
    };
    return new Dates();
}]);
