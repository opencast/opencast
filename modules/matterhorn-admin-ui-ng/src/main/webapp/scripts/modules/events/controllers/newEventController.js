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
 *
 * HINT: form validation uses hidden fields: unresolvedConflictsValidation, conditionalFormValidation
 *
 */
// Controller for creating a new event. This is a wizard, so it implements a state machine design pattern.
angular.module('adminNg.controllers')
.controller('NewEventCtrl', ['$scope', 'NewEventResource', 'Notifications', 'Modal',
    'NewEventMetadata', 'NewEventSource', 'NewEventProcessing', 'NewEventAccess', 'NewEventSummary', '$translate', 'RegexService',
 function ($scope, NewEventResource, Notifications, Modal,
           NewEventMetadata, NewEventSource, NewEventProcessing, NewEventAccess, NewEventSummary, $translate, RegexService) {

     $scope.metadataStep = NewEventMetadata;
     $scope.sourceStep = NewEventSource;
     $scope.processingStep = NewEventProcessing.get();
     $scope.accessStep = NewEventAccess;
     $scope.summaryStep = NewEventSummary;

     if (angular.isDefined($scope.metadataStep) && angular.isDefined($scope.accessStep)) {
         $scope.accessStep.setMetadata($scope.metadataStep);
     }

     $scope.states = [
         {name: 'metadata', stateController: $scope.metadataStep},
         {name: 'source', stateController: $scope.sourceStep},
         {name: 'processing', stateController: $scope.processingStep},
         {name: 'access', stateController: $scope.accessStep},
         {name: 'summary', stateController: $scope.summaryStep}
     ];


    // This is a hack, due to the fact that we need to read html from the server :(
    // Shall be banished ASAP
     var resetStates = function () {
         angular.forEach($scope.states, function(state)  {
             if (angular.isDefined(state.stateController.reset)) {
                 state.stateController.reset();
             }
         });
     };

    $scope.save = function () {
        // required but without logic
    };

    // translate the date patterns to a regex
    $scope.translateToPattern = function(key) {
        return RegexService.translateDateFormatToPattern($translate.instant(key));
    };

    $scope.submit = function () {
        var messageId, userdata = { metadata: []}, ace = [];

        window.onbeforeunload = function (e) {
            var confirmationMessage = 'The file has not completed uploading.';

            (e || window.event).returnValue = confirmationMessage;     //Gecko + IE
            return confirmationMessage;                                //Webkit, Safari, Chrome etc.
        };
        
        angular.forEach($scope.states, function (state) {

            if (state.stateController.isMetadataState) {
                for (var o in state.stateController.ud) {
                    if (state.stateController.ud.hasOwnProperty(o)) {
                        userdata.metadata.push(state.stateController.ud[o]);
                    }
                }
            } else if (state.stateController.isAccessState) {
                angular.forEach(state.stateController.ud.policies, function (policy) {
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

                        angular.forEach(policy.actions.value, function(customAction){
                           ace.push({
                                'action' : customAction,
                                'allow'  : true,
                                'role'   : policy.role
                           });
                        });
                    }
                });

                userdata.access = {
                    acl: {
                        ace: ace
                    }
                };
            } else {
                userdata[state.name] = state.stateController.ud;
            }
        });

        NewEventResource.save({}, userdata, function () {
            Notifications.add('success', 'EVENTS_CREATED');
            Notifications.remove(messageId);
            resetStates();
            window.onbeforeunload = null;
        }, function () {
            Notifications.add('error', 'EVENTS_NOT_CREATED');
            Notifications.remove(messageId);
            resetStates();
            window.onbeforeunload = null;
        });

        // close will also make the Table.fetch()
        Modal.$scope.close();
        // add message that never disappears
        messageId = Notifications.add('success', 'EVENTS_UPLOAD_STARTED', 'global', -1);
    };

 }]);
