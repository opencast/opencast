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
.controller('BulkMessageCtrl', ['$scope', 'BulkMessageStates', 'EmailResource', 'Table', 'Notifications', 'Modal',
  'EVENT_TAB_CHANGE',
  function ($scope, BulkMessageStates, EmailResource, Table, Notifications, Modal, EVENT_TAB_CHANGE) {
    BulkMessageStates.reset();
    $scope.states = BulkMessageStates.get();

    // Render the preview only when the summary tab has been reached.
    $scope.$on(EVENT_TAB_CHANGE, function (event, args) {
      if (args.current.name === 'summary') {
        $scope.states[1].stateController.updatePreview();
      }
    });

    $scope.submit = function () {
      var userdata = {};
      angular.forEach($scope.states, function (state) {
        userdata[state.name] = state.stateController.ud;
      });

      EmailResource.save({ templateId: userdata.message.email_template.id }, userdata, function () {
        Table.fetch();
        Table.deselectAll();
        Notifications.add('success', 'EMAIL_SENT');
        Modal.$scope.close();
      }, function () {
        Notifications.add('error', 'EMAIL_NOT_SENT', 'bulk-message-form');
      });
    };
  }]);
