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

// Controller for creating a new event. This is a wizard, so it implements a state machine design pattern.
angular.module('adminNg.controllers')
.controller('NewSeriesCtrl', ['$scope', 'NewSeriesStates', 'SeriesResource', 'ResourcesListResource', 'Notifications', 'Modal', 'Table',
        function ($scope, NewSeriesStates, SeriesResource, ResourcesListResource, Notifications, Modal, Table) {
    $scope.states = NewSeriesStates.get();

    function pushAllPropertiesIntoArray(object, array) {
        for (var o in object) {
            if (object.hasOwnProperty(o)) {
                array.push(object[o]);
            }
        }
    }

    // Post all the information collect by the wizard to create the new series
    $scope.submit = function () {
        var userdata, metadata = [], options = {}, access, theme, ace;
        // assemble the metadata from the two metadata controllers
        pushAllPropertiesIntoArray($scope.states[0].stateController.ud, metadata);
        if ($scope.states[1].name === 'metadata-extended') {
            pushAllPropertiesIntoArray($scope.states[1].stateController.ud, metadata);
        }

        // assemble the access
        if ($scope.states[1].name === 'access') {
            access = $scope.states[1].stateController.ud;
        } else if ($scope.states[2].name === 'access') {
            access = $scope.states[2].stateController.ud;
        }

        ace = [];
        angular.forEach(access.policies, function (policy) {
            if (angular.isDefined(policy.role)) {
                if (policy.read) {
                    ace.push({
                        'action' : 'read',
                        'allow'  : policy.read,
                        'role'   : policy.role
                    });
                }

                if (policy.write) {
                    ace.push({
                        'action' : 'write',
                        'allow'  : policy.write,
                        'role'   : policy.role
                    });   
                }
            }

        });

        userdata = {
                metadata: metadata,
                options:  options,
                access: {
                    acl: {
                        ace: ace
                    }
                }
        };

        // lastly, assemble the theme
        if ($scope.states[2].name === 'theme') {
            theme = $scope.states[2].stateController.ud.theme;
        }
        else if($scope.states[3].name === 'theme'){
            theme = $scope.states[3].stateController.ud.theme;
        }

        if (angular.isDefined(theme) && theme !== null && !angular.isObject(theme)) {
            userdata.theme = Number(theme);
        }


        SeriesResource.create({}, userdata, function () {
            Table.fetch();
            Notifications.add('success', 'SERIES_ADDED');

            // Reset all states
            angular.forEach($scope.states, function(state)  {
                if (angular.isDefined(state.stateController.reset)) {
                    state.stateController.reset();
                }
            });

            Modal.$scope.close();
        }, function () {
            Notifications.add('error', 'SERIES_NOT_SAVED', 'series-form');
        });
    };

    $scope.components = ResourcesListResource.get({ resource: 'components' });
}]);
