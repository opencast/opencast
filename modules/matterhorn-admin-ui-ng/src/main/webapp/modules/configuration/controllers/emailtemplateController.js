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

angular.module('adminNg.controllers')
.controller('EmailtemplateCtrl', ['$scope', 'EmailtemplateStates', 'EmailTemplateResource', 'Notifications', 'Modal', 'Table', 'EVENT_TAB_CHANGE',
        function ($scope, EmailtemplateStates, EmailTemplateResource, Notifications, Modal, Table, EVENT_TAB_CHANGE) {
    $scope.states = EmailtemplateStates.get();
    EmailtemplateStates.reset();

    // Render the preview only when the summary tab has been reached.
    $scope.$on(EVENT_TAB_CHANGE, function (event, args) {
        if (args.current.name === 'summary') {
            $scope.states[0].stateController.updatePreview();
        }
    });

    // Populate user data if the email template is being edited
    if ($scope.action === 'edit') {
        EmailTemplateResource.get({ id: Modal.$scope.resourceId }, function (emailtemplate) {
            // Populate the message step
            $scope.states[0].stateController.ud = emailtemplate;
        });
    }

    $scope.submit = function () {
        var userdata = {};
        angular.forEach($scope.states, function (state) {
            userdata[state.name] = state.stateController.ud;
        });

        if ($scope.action === 'edit') {
            EmailTemplateResource.update({ id: Modal.$scope.resourceId }, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'EMAIL_TEMPLATE_SAVED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'EMAIL_TEMPLATE_NOT_SAVED', 'emailtemplate-form');
            });
        } else {
            EmailTemplateResource.save({}, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'EMAIL_TEMPLATE_CREATED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'EMAIL_TEMPLATE_NOT_CREATED', 'emailtemplate-form');
            });
        }
    };
}]);
