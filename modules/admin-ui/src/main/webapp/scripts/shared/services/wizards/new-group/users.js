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
.factory('NewGroupUsers', ['ResourcesListResource', '$location', function (ResourcesListResource) {
    var Users = function () {
        var me = this;
        me.users = {
            available: ResourcesListResource.query({ resource: 'USERS.INVERSE'}),
            selected:  [],
            i18n: 'USERS.GROUPS.DETAILS.USERS',
            searchable: true
        };

        this.reset = function () {
        };
        this.reset();

        this.isValid = function () {
            return true;
        };

        this.getUsersList = function () {
            var list = '';

            angular.forEach(me.users.selected, function (user, index) {
                list += user.name + ((index + 1) === me.users.selected.length ? '' : ', ');
            });

            return list;
        };
    };
    return new Users();
}]);
