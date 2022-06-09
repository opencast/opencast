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
.controller('AclCtrl', ['$scope', 'AclResource', 'RolesResource', 'ResourcesListResource',
  'UserResource', 'UsersResource', 'Notifications', 'Modal',
  function ($scope, AclResource, RolesResource, ResourcesListResource,
    UserResource, UsersResource, Notifications, Modal) {

    var createPolicy = function (role, read, write) {
          return {
            role  : role,
            read  : read !== undefined ? read : true,
            write : write !== undefined ? write : false,
            actions : {
              name : 'edit-acl-actions',
              value : []
            },
            user: undefined,
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
            if (!policy.role.startsWith($scope.roleUserPrefix)) {
              $scope.policies.push(policy);
            }
          });

          $scope.policiesUser = [];
          angular.forEach(newPolicies, function (policy) {
            if (policy.role.startsWith($scope.roleUserPrefix)) {
              var id = policy.role.split($scope.roleUserPrefix).pop().toLowerCase();

              UserResource.get({ username: id }).$promise.then(function (data) {
                policy.user = data;
              });

              $scope.policiesUser.push(policy);
            }
          });

          if (!loading) {
            $scope.save();
          }
        };

    $scope.policies = [];
    $scope.policiesUser = [];
    $scope.baseAcl = {};
    $scope.metadata = {};

    $scope.changeBaseAcl = function () {
      $scope.baseAcl = AclResource.get({id: this.baseAclId}, function () {
        changePolicies($scope.baseAcl.acl.ace);
      });
      this.baseAclId = '';
    };

    $scope.filterUserRoles = function (item) {
      if (!item) {
        return true;
      }
      return !item.includes($scope.roleUserPrefix);
    };

    $scope.userToStringForDetails = function (user) {
      if (!user) {
        return undefined;
      }
      var n = user.name ? user.name : user.username;
      var e = user.email ? '<' + user.email + '>' : '';

      return n + ' ' + e;
    };

    $scope.getAllPolicies = function () {
      return [].concat($scope.policies, $scope.policiesUser);
    };

    // E.g. model === $scope.policies
    $scope.addPolicy = function (model) {
      model.push(createPolicy(
        undefined,
        $scope.aclCreateDefaults['read_enabled'],
        $scope.aclCreateDefaults['write_enabled']
      ));
    };

    // E.g. model === $scope.policies
    $scope.deletePolicy = function (model, policyToDelete) {
      var index;

      angular.forEach(model, function (policy, idx) {
        if (policy.role === policyToDelete.role &&
                policy.write === policyToDelete.write &&
                policy.read === policyToDelete.read) {
          index = idx;
        }
      });

      if (angular.isDefined(index)) {
        model.splice(index, 1);
      }

      $scope.save();
    };

    $scope.getMatchingRoles = function (value) {
      RolesResource.queryNameOnly({query: value, target:'ACL'}).$promise.then(function (data) {
        angular.forEach(data, function(newRole) {
          if ($scope.roles.indexOf(newRole) == -1) {
            $scope.roles.unshift(newRole);
          }
        });
      });
    };

    $scope.getMatchingUsers = function (value) {
      UsersResource.query({query: value}).$promise.then(function (data) {
        angular.forEach(data, function(newRole) {
          if ($scope.roles.indexOf(newRole) == -1) {
            $scope.roles.unshift(newRole);
          }
        });
      });
    };

    fetchChildResources = function (id) {
      //NB: roles is updated in both the functions for $scope.acl (MH-11716) and $scope.roles (MH-11715, MH-11717)
      $scope.roles = RolesResource.queryNameOnly({limit: -1, target: 'ACL'});

      $scope.aclCreateDefaults = {};
      $scope.aclCreateDefaults = ResourcesListResource.get({ resource: 'ACL.DEFAULTS'}, function(data) {
        angular.forEach(data, function (value, key) {
          if (key.charAt(0) !== '$') {
            $scope.aclCreateDefaults[key] = value;
          }
        });

        $scope.aclCreateDefaults['read_enabled'] = $scope.aclCreateDefaults['read_enabled'] !== undefined
          ? ($scope.aclCreateDefaults['read_enabled'].toLowerCase() === 'true') : true;
        $scope.aclCreateDefaults['write_enabled'] = $scope.aclCreateDefaults['write_enabled'] !== undefined
          ? ($scope.aclCreateDefaults['write_enabled'].toLowerCase() === 'true') : false;
        $scope.aclCreateDefaults['read_readonly'] = $scope.aclCreateDefaults['read_readonly'] !== undefined
          ? ($scope.aclCreateDefaults['read_readonly'].toLowerCase() === 'true') : true;
        $scope.aclCreateDefaults['write_readonly'] = $scope.aclCreateDefaults['write_readonly'] !== undefined
          ? ($scope.aclCreateDefaults['write_readonly'].toLowerCase() === 'true') : false;
        $scope.roleUserPrefix = $scope.aclCreateDefaults['role_user_prefix'] !== undefined
          ? $scope.aclCreateDefaults['role_user_prefix']
          : 'ROLE_USER_';
      });

      $scope.acl = AclResource.get({id: id}, function (data) {
        // Load settings before creating the policy lists
        $scope.aclCreateDefaults.$promise.then(function () {
          $scope.metadata.name = data.name;

          if (angular.isDefined(data.acl)) {
            var json = angular.fromJson(data.acl);
            changePolicies(json.ace, true);
          }

          angular.forEach(angular.fromJson(data.acl.ace), function(value, key) {
            var roleName = value['role'];
            if ($scope.roles.indexOf(roleName) == -1) {
              $scope.roles.push(roleName);
            }
          }, this);
        });
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

      $scope.users = UsersResource.query({limit: 2147483647});
      $scope.users.$promise.then(function () {
        var newUsers = [];
        angular.forEach($scope.users.rows, function(user) {
          user.userRole = $scope.roleUserPrefix + user.username.replace(/\W/g, '').toUpperCase();
          newUsers.push(user);
        });
        $scope.users = newUsers;
      });
    };

    fetchChildResources($scope.resourceId);

    $scope.$on('change', function (event, id) {
      fetchChildResources(id);
    });

    $scope.submit = function () {
      var ace = [];

      angular.forEach($scope.getAllPolicies(), function (policy) {
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
