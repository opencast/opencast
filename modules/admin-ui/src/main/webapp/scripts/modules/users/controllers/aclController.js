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

// Controller for all single series screens.
angular.module('adminNg.controllers')
.controller('AclCtrl', ['$scope', 'AclResource', 'UserRolesResource', 'ResourcesListResource', 'Notifications', 'Modal',
  function ($scope, AclResource, UserRolesResource, ResourcesListResource, Notifications, Modal) {
    var roleSlice = 100;
    var roleOffset = 0;
    var loading = false;
    var rolePromise = null;

    var createPolicy = function (role) {
          return {
            role  : role,
            read  : false,
            write : false,
            actions : {
              name : 'edit-acl-actions',
              value : []
            }
          };
        },
        fetchChildResources,
        changePolicies = function (access, loading) {
          var newPolicies = {};
          angular.forEach(access, function (acl) {
            var policy = newPolicies[acl.role];

            if (angular.isUndefined(policy)) {
              newPolicies[acl.role] = createPolicy(acl.role);
            }
            if (acl.action === 'read' || acl.action === 'write') {
              newPolicies[acl.role][acl.action] = acl.allow;
            } else if (acl.allow === true || acl.allow === 'true'){
              newPolicies[acl.role].actions.value.push(acl.action);
            }
          });

          $scope.policies = [];
          angular.forEach(newPolicies, function (policy) {
            $scope.policies.push(policy);
          });

          if (!loading) {
            $scope.save();
          }
        };

    $scope.policies = [];
    $scope.baseAcl = {};
    $scope.metadata = {};

    $scope.changeBaseAcl = function () {
      $scope.baseAcl = AclResource.get({id: this.baseAclId}, function () {
        changePolicies($scope.baseAcl.acl.ace);
      });
      this.baseAclId = '';
    };

    $scope.addPolicy = function () {
      $scope.policies.push(createPolicy());
    };

    $scope.deletePolicy = function (policyToDelete) {
      var index;

      angular.forEach($scope.policies, function (policy, idx) {
        if (policy.role === policyToDelete.role &&
                policy.write === policyToDelete.write &&
                policy.read === policyToDelete.read) {
          index = idx;
        }
      });

      if (angular.isDefined(index)) {
        $scope.policies.splice(index, 1);
      }

      $scope.save();
    };

    $scope.getMoreRoles = function (value) {

      if (loading)
        return rolePromise;

      loading = true;
      var queryParams = {limit: roleSlice, offset: roleOffset};

      if ( angular.isDefined(value) && (value != '')) {
        //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
        //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
        queryParams['filter'] = 'role_name:' + value + ',role_target:ACL';
        queryParams['offset'] = 0;
      } else {
        queryParams['filter'] = 'role_target:ACL';
      }
      rolePromise = UserRolesResource.query(queryParams);
      rolePromise.$promise.then(function (data) {
        angular.forEach(data, function (role) {
          $scope.roles[role.name] = role.value;
        });
        roleOffset = Object.keys($scope.roles).length;
      }).catch(
        angular.noop
      ).finally(function () {
        loading = false;
      });
      return rolePromise;
    };

    fetchChildResources = function (id) {
      //NB: roles is updated in both the functions for $scope.acl (MH-11716) and $scope.roles (MH-11715, MH-11717)
      $scope.roles = {};
      $scope.acl = AclResource.get({id: id}, function (data) {
        $scope.metadata.name = data.name;

        if (angular.isDefined(data.acl)) {
          var json = angular.fromJson(data.acl);
          changePolicies(json.ace, true);
        }

        angular.forEach(angular.fromJson(data.acl.ace), function(value, key) {
          var rolename = value['role'];
          if (angular.isUndefined($scope.roles[rolename])) {
            $scope.roles[rolename] = rolename;
          }
        }, this);
      });

      $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
      $scope.actions = {};
      $scope.hasActions = false;
      ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
        angular.forEach(data, function (value, key) {
          if (key.charAt(0) !== '$') {
            $scope.actions[key] = value;
            $scope.hasActions = true;
          }
        });
      });
      $scope.getMoreRoles();
    };


    fetchChildResources($scope.resourceId);

    $scope.$on('change', function (event, id) {
      fetchChildResources(id);
    });

    $scope.submit = function () {
      var ace = [];

      angular.forEach($scope.policies, function (policy) {
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

      AclResource.save({id: $scope.resourceId}, {
        acl: {
          ace: ace
        },
        name: $scope.metadata.name
      }, function () {
        Notifications.add('success', 'ACL_UPDATED');
        Modal.$scope.close();
      }, function () {
        Notifications.add('error', 'ACL_NOT_SAVED', 'acl-form');
      });
    };
  }]);
