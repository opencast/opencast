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
.factory('NewLocationblacklistItems', ['CaptureAgentsResource', function (CaptureAgentsResource) {
    var Locations = function () {
        var me = this;

        this.reset = function () {
            me.ud = { items: [] };
        };
        this.reset();

        this.isValid = function () {
            return me.ud.items.length > 0;
        };

        this.items = CaptureAgentsResource.query();

        this.addItem = function () {
            var found = false;
            angular.forEach(me.ud.items, function (item) {
                if (item.id === me.ud.itemToAdd.id) {
                    found = true;
                }
            });
            if (!found) {
                me.ud.items.push(me.ud.itemToAdd);
                me.ud.itemToAdd = {};
            }
        };

        // Selecting multiple blacklistedIds is not yet supported by
        // the back end.

        //this.selectAll = function () {
        //    angular.forEach(me.ud.items, function (item) {
        //        item.selected = me.all;
        //    });
        //};

        //this.removeItem = function () {
        //    var items = [];
        //    angular.forEach(me.ud.items, function (item) {
        //        if (!item.selected) {
        //            items.push(item);
        //        }
        //    });
        //    me.ud.items = items;
        //};
    };
    return new Locations();
}]);
