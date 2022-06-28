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
.factory('NewSeriesAccess', ['ResourcesListResource', 'SeriesAccessResource', 'AuthService', 'RolesResource',
  'UserResource', 'UsersResource', 'Notifications', '$timeout',
  function (ResourcesListResource, SeriesAccessResource, AuthService, RolesResource, UserResource, UsersResource,
    Notifications, $timeout) {
    var Access = function () {

      var me = this,
          NOTIFICATION_CONTEXT = 'series-acl',
          aclNotification,
          createPolicy = function (role, read, write) {
            return {
              role  : role,
              read  : read !== undefined ? read : true,
              write : write !== undefined ? write : false,
              actions : {
                name : 'new-series-acl-actions',
                value : []
              },
              user: undefined,
            };
          },
          checkNotification = function () {
            if (me.unvalidRule) {
              if (!angular.isUndefined(me.notificationRules)) {
                Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
              }
              me.notificationRules = Notifications.add('warning', 'INVALID_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRules)) {
              Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
              me.notificationRules = undefined;
            }

            if (!me.hasRights) {
              if (!angular.isUndefined(me.notificationRights)) {
                Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
              }
              me.notificationRights = Notifications.add('warning', 'MISSING_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRights)) {
              Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
              me.notificationRights = undefined;
            }

            $timeout(function () {
              checkNotification();
            }, 200);
          },
          addUserRolePolicy = function (policies) {
            if (angular.isDefined(AuthService.getUserRole())) {
              var currentUserPolicy = createPolicy(AuthService.getUserRole());
              currentUserPolicy.read = true;
              currentUserPolicy.write = true;
              currentUserPolicy.userRole = currentUserPolicy.role;
              policies.push(currentUserPolicy);
            }
            return policies;
          };

      me.ud = {};
      me.ud.id = {};
      me.ud.policies = [];
      me.ud.policiesUser = [];
      // Add the current user's role to the ACL upon the first startup
      me.ud.policiesUser = addUserRolePolicy(me.ud.policiesUser);
      me.ud.baseAcl = {};

      this.changeBaseAcl = function () {
        var newPolicies = {};
        me.ud.baseAcl = SeriesAccessResource.getManagedAcl({id: me.ud.id}, function () {
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

          me.ud.policies = [];
          // After loading an ACL template add the user's role to the top of the ACL list if it isn't included
          if (angular.isDefined(AuthService.getUserRole())
            && !angular.isDefined(newPolicies[AuthService.getUserRole()])) {
            me.ud.policiesUser = addUserRolePolicy(me.ud.policiesUser);
          }
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
              }).catch(function() {
                // User does not exist, remove associated policy from list
                var index = me.ud.policiesUser.indexOf(policy);
                if (index !== -1) {
                  me.ud.policiesUser.splice(index, 1);
                }
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
        var hasRights = false,
            rulesValid = true;

        angular.forEach(this.getAllPolicies(), function (policy) {
          if (policy.read && policy.write) {
            hasRights = true;
          }

          if (!(policy.read || policy.write || policy.actions.value.length > 0) || angular.isUndefined(policy.role)) {
            rulesValid = false;
          }
        });

        me.unvalidRule = !rulesValid;
        me.hasRights = hasRights;

        return rulesValid && hasRights;
      };

      checkNotification();

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

      me.roles = RolesResource.queryNameOnly({limit: -1, target: 'ACL'});

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

      this.reset = function () {
        me.ud = {
          id: {},
          policies: [],
          policiesUser: [],
        };
        // Add the user's role upon resetting
        me.ud.policiesUser = addUserRolePolicy(me.ud.policiesUser);
      };

      this.reset();
    };

    return new Access();
  }]);
