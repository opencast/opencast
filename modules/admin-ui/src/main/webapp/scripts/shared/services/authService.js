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
.factory('AuthService', ['IdentityResource', function (IdentityResource) {
    var AuthService = function () {
        var me = this,
            isAdmin = false,
            isOrgAdmin = false,
            isUserLoaded = false,
            callbacks = [],
            identity,
            userRole,
            isAuthorizedAs = function (role) {
                if (angular.isUndefined(me.user.roles)) {
                    return false;
                }
                return isAdmin ||
                    (angular.isArray(me.user.roles) && me.user.roles.indexOf(role) > -1) ||
                    me.user.roles === role;
            };

        this.user = {};

        this.loadUser = function () {
            identity = IdentityResource.get();
            identity.$promise.then(function (user) {
                // Users holding the global admin role shall always be authorized to do anything
                var globalAdminRole = "ROLE_ADMIN";
                me.user = user;
                isAdmin = angular.isDefined(globalAdminRole) && user.roles.indexOf(globalAdminRole) > -1;
                isOrgAdmin = angular.isDefined(user.org.adminRole) && user.roles.indexOf(user.org.adminRole) > -1;
                if (angular.isDefined(user.userRole)) {
                    userRole = user.userRole;
                }
                isUserLoaded = true;
                angular.forEach(callbacks, function (item) {
                    isAuthorizedAs(item.role) ? item.success() : item.error();
                });
            });
        };

        this.getUser = function () {
            return identity;
        };

        this.getUserRole = function () {
            return userRole;
        };

        this.isOrganizationAdmin = function () {
            return isOrgAdmin;
        };

        this.userIsAuthorizedAs = function (role, success, error) {
            if (angular.isUndefined(success)) {
                return isAuthorizedAs(role);
            }

            if (isUserLoaded) {
                isAuthorizedAs(role) ? success() : error();
            } else {
                callbacks.push({
                    role    : role,
                    success : success,
                    error   : error
                });
            }
        };

        this.loadUser();
    };

    return new AuthService();
}]);
