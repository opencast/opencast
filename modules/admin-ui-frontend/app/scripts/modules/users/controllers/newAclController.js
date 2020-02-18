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
.controller('NewAclCtrl', ['$scope', 'Table', 'NewAclStates', 'ResourcesListResource', 'AclsResource', 'Notifications',
  'Modal', function ($scope, Table, NewAclStates, ResourcesListResource, AclsResource, Notifications, Modal) {

    $scope.states = NewAclStates.get();

    $scope.submit = function () {
      var access = $scope.states[1].stateController.ud,
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

          angular.forEach(policy.actions.value, function(customAction){
            ace.push({
              'action' : customAction,
              'allow'  : true,
              'role'   : policy.role
            });
          });
        }

      });

      $scope.acl = {
        name : $scope.states[0].stateController.metadata.name,
        acl  : {
          ace: ace
        }
      };

      AclsResource.create($scope.acl, function () {
        Table.fetch();
        Modal.$scope.close();

        // Reset all states
        angular.forEach($scope.states, function(state)  {
          if (angular.isDefined(state.stateController.reset)) {
            state.stateController.reset();
          }
        });

        Notifications.add('success', 'ACL_ADDED');
      }, function () {
        Notifications.add('error', 'ACL_NOT_SAVED', 'acl-form');
      });

    };
  }
]);
