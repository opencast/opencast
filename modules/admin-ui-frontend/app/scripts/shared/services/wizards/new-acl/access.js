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
.factory('NewAclAccess', ['ResourcesListResource', 'RolesResource', 'AclResource', 'UserResource', 'UsersResource',
  function (ResourcesListResource, RolesResource, AclResource, UserResource, UsersResource) {
    var Access = function () {

      var me = this,
          createPolicy = function (role, read, write) {
            return {
              role  : role,
              read  : read !== undefined ? read : true,
              write : write !== undefined ? write : false,
              actions : {
                name : 'new-acl-actions',
                value : []
              },
              user: undefined,
            };
          };

      me.isAccessState = true;
      me.ud = {};
      me.ud.id = {};
      me.ud.policies = [];
      me.ud.policiesUser = [];
      me.ud.baseAcl = {};

      this.changeBaseAcl = function () {
        var newPolicies = {};
        me.ud.baseAcl = AclResource.get({id: me.ud.id}, function () {
          angular.forEach(me.ud.baseAcl.acl.ace, function (acl) {
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

          angular.forEach(newPolicies, function (policy) {
            if (!policy.role.startsWith(me.roleUserPrefix)) {
              me.ud.policies.push(policy);
            }
          });

          me.ud.policiesUser = [];
          angular.forEach(newPolicies, function (policy) {
            if (policy.role.startsWith(me.roleUserPrefix)) {
              var id = policy.role.split(me.roleUserPrefix).pop().toLowerCase();

              UserResource.get({ username: id }).$promise.then(function (data) {
                policy.user = data;
              });

              me.ud.policiesUser.push(policy);
            }
          });

          me.ud.id = '';
        });
      };

      this.filterUserRoles = function (item) {
        if (!item) {
          return true;
        }
        return !item.includes(me.roleUserPrefix);
      };

      this.userToStringForDetails = function (user) {
        if (!user) {
          return undefined;
        }
        var n = user.name ? user.name : user.username;
        var e = user.email ? '<' + user.email + '>' : '';

        return n + ' ' + e;
      };

      this.getAllPolicies = function () {
        return [].concat(me.ud.policies, me.ud.policiesUser);
      };

      // E.g. model === $scope.policies
      this.addPolicy = function (model) {
        model.push(createPolicy(
          undefined,
          me.aclCreateDefaults['read_enabled'],
          me.aclCreateDefaults['write_enabled']
        ));
      };

      // E.g. model === $scope.policies
      this.deletePolicy = function (model, policyToDelete) {
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
      };

      this.isValid = function () {
        // Is always true, the series can have an empty ACL
        return true;
      };

      me.acls  = ResourcesListResource.get({ resource: 'ACL' });
      me.actions = {};
      me.hasActions = false;
      ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
        angular.forEach(data, function (value, key) {
          if (key.charAt(0) !== '$') {
            me.actions[key] = value;
            me.hasActions = true;
          }
        });
      });

      me.aclCreateDefaults = {};
      ResourcesListResource.get({ resource: 'ACL.DEFAULTS'}, function(data) {
        angular.forEach(data, function (value, key) {
          if (key.charAt(0) !== '$') {
            me.aclCreateDefaults[key] = value;
          }
        });

        me.aclCreateDefaults['read_enabled'] = me.aclCreateDefaults['read_enabled'] !== undefined
          ? (me.aclCreateDefaults['read_enabled'].toLowerCase() === 'true') : true;
        me.aclCreateDefaults['write_enabled'] = me.aclCreateDefaults['write_enabled'] !== undefined
          ? (me.aclCreateDefaults['write_enabled'].toLowerCase() === 'true') : false;
        me.aclCreateDefaults['read_readonly'] = me.aclCreateDefaults['read_readonly'] !== undefined
          ? (me.aclCreateDefaults['read_readonly'].toLowerCase() === 'true') : true;
        me.aclCreateDefaults['write_readonly'] = me.aclCreateDefaults['write_readonly'] !== undefined
          ? (me.aclCreateDefaults['write_readonly'].toLowerCase() === 'true') : false;
        me.roleUserPrefix = me.aclCreateDefaults['role_user_prefix'] !== undefined
          ? me.aclCreateDefaults['role_user_prefix']
          : 'ROLE_USER_';
      });

      me.users = UsersResource.query({limit: 2147483647});
      me.users.$promise.then(function () {
        var newUsers = [];
        angular.forEach(me.users.rows, function(user) {
          user.userRole = me.roleUserPrefix + user.username.replace(/\W/g, '').toUpperCase();
          newUsers.push(user);
        });
        me.users = newUsers;
      });

      this.getMatchingRoles = function (value) {
        RolesResource.queryNameOnly({query: value, target: 'ACL'}).$promise.then(function (data) {
          angular.forEach(data, function(newRole) {
            if (me.roles.indexOf(newRole) == -1) {
              me.roles.unshift(newRole);
            }
          });
          return;
        });
      };

      this.getMatchingUsers = function (value) {
        UsersResource.query({query: value}).$promise.then(function (data) {
          angular.forEach(data, function(newRole) {
            if (me.roles.indexOf(newRole) == -1) {
              me.roles.unshift(newRole);
            }
          });
        });
      };

      me.roles = RolesResource.queryNameOnly({limit: -1, target: 'ACL'});

      this.reset = function () {
        me.ud = {
          id: {},
          policies: [],
          policiesUser: [],
        };
      };

      this.reset();
    };
    return new Access();
  }
]);
