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

// A controller for global page navigation
angular.module('adminNg.controllers')
.controller('UserPreferencesCtrl', ['$scope', 'SignatureResource', 'IdentityResource',
    function ($scope, SignatureResource, IdentityResource) {
        var persist = function (persistenceMethod) {
            var me = IdentityResource.get();
            me.$promise.then(function (data) {
                $scope.userprefs.username = data.username;
                SignatureResource[persistenceMethod]($scope.userprefs);
            });
        };

        // load the current user preferences
        $scope.userprefs = SignatureResource.get({});

        // perform update
        $scope.update = function () {
            if ($scope.userPrefForm.$valid) {
                persist('update');
                $scope.close();
            }
        };


        // perform save
        $scope.save = function () {
            if ($scope.userPrefForm.$valid) {
                persist('save');
                $scope.close();
            }
        };
    }
]);

