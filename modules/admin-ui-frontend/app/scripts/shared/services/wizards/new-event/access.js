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
.factory('NewEventAccess', ['ResourcesListResource', 'EventAccessResource', 'SeriesAccessResource', 'AuthService',
  'RolesResource', 'UserResource', 'UsersResource', 'Notifications', '$timeout',
  function (ResourcesListResource, EventAccessResource, SeriesAccessResource, AuthService, RolesResource,
    UserResource, UsersResource, Notifications, $timeout) {
    var Access = function () {

      var me = this;
      var NOTIFICATION_CONTEXT = 'events-access';
      var createPolicy = function (role, read, write, actionValues) {
        return {
          role  : role,
          read  : read !== undefined ? read : false,
          write : write !== undefined ? write : false,
          actions : {
            name : 'new-event-acl-actions',
            value : actionValues !== undefined ? actionValues.slice() : [],
          },
          user: undefined,
        };
      };

      var addUserRolePolicy = function (policies) {
        if (angular.isDefined(AuthService.getUserRole())) {
          var currentUserPolicy = createPolicy(AuthService.getUserRole(), true, true,
            me.aclCreateDefaults['default_actions']);
          currentUserPolicy.userRole = currentUserPolicy.role;
          policies.push(currentUserPolicy);
        }
        return policies;
      };

      var changePolicies = function (access, loading) {
        if (!Array.isArray(access)) {
          access = [access];
        }
        var newPolicies = {},
            foundUserRole = false;
        angular.forEach(access, function (acl) {
          var policy = newPolicies[acl.role];

          if (angular.isUndefined(policy)) {
            newPolicies[acl.role] = createPolicy(acl.role);
          }

          if (acl.action === 'read' || acl.action === 'write') {
            newPolicies[acl.role][acl.action] = acl.allow;
          } else if (acl.allow === true || acl.allow === 'true'){
            // Handle additional ACL actions
            newPolicies[acl.role].actions.value.push(acl.action);
          }

          if (acl.role === AuthService.getUserRole()) {
            foundUserRole = true;
          }

          if (me.roles.indexOf(acl.role) == -1) {
            me.roles.push(acl.role);
          }
        });

        me.ud.policies = [];
        // Add user role if not already present in Series ACL
        if (!foundUserRole) {
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
            var id = policy.role.split(me.roleUserPrefix).pop();
            // FIXMe: If roles are sanitized, WE CANNOT derive the user from their user role,
            //  because the user roles are converted to uppercase, while usernames are case sensitive.
            //  This is a terrible workaround, because usernames are usually all lowercase anyway.
            if (me.aclCreateDefaults['sanitize']) {
              id = id.toLowerCase();
            }

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

        if (!loading) {
          me.ud.accessSave();
        }
      };

      var checkNotification = function () {
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
      };

      me.isAccessState = true;
      me.ud = {};
      me.ud.id = undefined;
      me.ud.policies = [];
      me.ud.policiesUser = [];
      me.ud.baseAcl = {};

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

        if (!(user.name || user.username) && user.userRole) {
          return user.userRole;
        }

        var n = user.name ? user.name : user.username;
        var e = user.email ? '<' + user.email + '>' : '';

        return n + ' ' + e;
      };

      AuthService.getUser().$promise.then(function (user) {
        var ADMIN_INIT_EVENT_ACL_WITH_SERIES_ACL = 'admin.init.event.acl.with.series.acl';
        if (angular.isDefined(user.org.properties[ADMIN_INIT_EVENT_ACL_WITH_SERIES_ACL])) {
          me.initEventAclWithSeriesAcl = user.org.properties[ADMIN_INIT_EVENT_ACL_WITH_SERIES_ACL] === 'true';
        } else {
          me.initEventAclWithSeriesAcl = true;
        }
      }).catch(angular.noop);

      this.filterUserRoles = function (item) {
        if (!item) {
          return true;
        }
        return !item.includes(me.roleUserPrefix);
      };

      this.setMetadata = function (metadata) {
        me.metadata = metadata;
      };

      this.loadSeriesAcl = function () {
        if (me.initEventAclWithSeriesAcl) {
          angular.forEach(me.metadata.getUserEntries(), function (m) {
            if (m.id === 'isPartOf' && angular.isDefined(m.value) && m.value !== '') {
              SeriesAccessResource.get({ id: m.value }, function (data) {
                if (angular.isDefined(data.series_access)) {
                  var json = angular.fromJson(data.series_access.acl);
                  changePolicies(json.acl.ace, true);
                }
              });
            }
          });
        }
      };

      this.changeBaseAcl = function (id) {
        // Get the policies which should persist on template change
        var allPolicies = this.getAllPolicies();
        var remainingPolicies = allPolicies.filter(policy => (
          me.aclCreateDefaults['keep_on_template_switch_role_prefixes'].some(
            pattern => policy.role.startsWith(pattern)
          )
        ));

        var remainingACEs = [];
        angular.forEach(remainingPolicies, function (policy) {
          if (policy.read) {
            remainingACEs.push({role: policy.role, action: 'read', allow: true});
          }
          if (policy.write) {
            remainingACEs.push({role: policy.role, action: 'write', allow: true});
          }
          for (const action of policy.actions.value) {
            remainingACEs.push({role: policy.role, action: action, allow: true});
          }
        });

        me.ud.id = id;
        var newPolicies = {};
        me.ud.baseAcl = EventAccessResource.getManagedAcl({id: me.ud.id}, function () {
          var combine = me.ud.baseAcl.acl.ace.concat(remainingACEs);

          angular.forEach(combine, function (acl) {
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
          me.ud.policiesUser = [];
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

          angular.forEach(newPolicies, function (policy) {
            if (policy.role.startsWith(me.roleUserPrefix)) {
              var id = policy.role.split(me.roleUserPrefix).pop().toLowerCase();

              UserResource.get({ username: id }).$promise.then(function (data) {
                policy.user = data;
                // Did we get a user not present in Opencast (e.g. LDAP)? Add it to the list!
                if (me.users.map(user => user.id).indexOf(id) == -1) {
                  if (me.aclCreateDefaults['sanitize']) {
                    // FixMe: See the FixMe above pertaining to sanitize
                    data.userRole = me.roleUserPrefix + id.replace(/\W/g, '_').toUpperCase();
                  } else {
                    data.userRole = me.roleUserPrefix + id;
                  }
                  me.users.push(data);
                }

                me.ud.policiesUser.push(policy);
              });
            }
          });
        });
      };

      this.getAllPolicies = function () {
        return [].concat(me.ud.policies, me.ud.policiesUser);
      };

      // E.g. model === $scope.policies
      this.addPolicy = function (model) {
        model.push(createPolicy(
          undefined,
          me.aclCreateDefaults['read_enabled'],
          me.aclCreateDefaults['write_enabled'],
          me.aclCreateDefaults['default_actions']
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

      me.aclCreateDefaults = ResourcesListResource.get({ resource: 'ACL.DEFAULTS'}, function(data) {
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
        me.aclCreateDefaults['default_actions'] = me.aclCreateDefaults['default_actions'] !== undefined
          ? me.aclCreateDefaults['default_actions'].split(',') : [];

        me.roleUserPrefix = me.aclCreateDefaults['role_user_prefix'] !== undefined
          ? me.aclCreateDefaults['role_user_prefix']
          : 'ROLE_USER_';
        me.aclCreateDefaults['sanitize'] = me.aclCreateDefaults['sanitize'] !== undefined
          ? (me.aclCreateDefaults['sanitize'].toLowerCase() === 'true') : true;
        me.aclCreateDefaults['keep_on_template_switch_role_prefixes'] =
          me.aclCreateDefaults['keep_on_template_switch_role_prefixes'] !== undefined
            ? me.aclCreateDefaults['keep_on_template_switch_role_prefixes'].split(',') : [];

        // Add the user's role upon creating the ACL editor - but only after defaults have loaded
        me.ud.policiesUser = addUserRolePolicy(me.ud.policiesUser);
      });

      me.roles = RolesResource.queryNameOnly({limit: -1, target: 'ACL'});

      me.users = UsersResource.query({limit: 2147483647});
      me.users.$promise.then(function () {
        me.aclCreateDefaults.$promise.then(function () {
          var newUsers = [];
          angular.forEach(me.users.rows, function(user) {
            if (me.aclCreateDefaults['sanitize']) {
              // FixMe: See the FixMe above pertaining to sanitize
              user.userRole = me.roleUserPrefix + user.username.replace(/\W/g, '_').toUpperCase();
            } else {
              user.userRole = me.roleUserPrefix + user.username;
            }
            newUsers.push(user);
          });
          me.users = newUsers;
        });
      });

      this.getMatchingRoles = function (value) {
        RolesResource.queryNameOnly({query: value, target: 'ACL'}).$promise.then(function (data) {
          angular.forEach(data, function(newRole) {
            if (me.roles.indexOf(newRole) == -1) {
              me.roles.unshift(newRole);

              // So we can have user roles that match custom role patterns
              if (newRole.startsWith(me.roleUserPrefix) ) {
                var user = {};
                user.userRole = newRole;
                me.users.push(user);
              }
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
        // Add user role after reset
        me.ud.policiesUser = addUserRolePolicy(me.ud.policiesUser);
      };
    };
    return new Access();
  }]);
