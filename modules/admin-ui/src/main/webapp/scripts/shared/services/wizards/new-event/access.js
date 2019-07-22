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
  'UserRolesResource', 'Notifications', '$timeout',
  function (ResourcesListResource, EventAccessResource, SeriesAccessResource, AuthService, UserRolesResource,
    Notifications, $timeout) {
    var Access = function () {

      var roleSlice = 100;
      var roleOffset = 0;
      var loading = false;
      var rolePromise = null;

      var me = this;
      var NOTIFICATION_CONTEXT = 'events-access';
      var createPolicy = function (role) {
        return {
          role  : role,
          read  : false,
          write : false,
          actions : {
            name : 'new-event-acl-actions',
            value : []
          }
        };
      };

      var addUserRolePolicy = function (policies) {
        if (angular.isDefined(AuthService.getUserRole())) {
          var currentUserPolicy = createPolicy(AuthService.getUserRole());
          currentUserPolicy.read = true;
          currentUserPolicy.write = true;
          policies.push(currentUserPolicy);
        }
        return policies;
      };

      var changePolicies = function (access, loading) {
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

          if (angular.isUndefined(me.roles[acl.role])) {
            me.roles[acl.role] = acl.role;
          }
        });

        me.ud.policies = [];
        // Add user role if not already present in Series ACL
        if (!foundUserRole) {
          me.ud.policies = addUserRolePolicy(me.ud.policies);
        }
        angular.forEach(newPolicies, function (policy) {
          me.ud.policies.push(policy);
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
      me.ud.id = {};
      me.ud.policies = [];
      // Add the user's role upon creating the ACL editor
      me.ud.policies = addUserRolePolicy(me.ud.policies);
      me.ud.baseAcl = {};

      this.setMetadata = function (metadata) {
        me.metadata = metadata;
        me.loadSeriesAcl();
      };

      this.loadSeriesAcl = function () {
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
      };

      this.changeBaseAcl = function () {
        var newPolicies = {};
        me.ud.baseAcl = EventAccessResource.getManagedAcl({id: me.ud.id}, function () {
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
            me.ud.policies = addUserRolePolicy(me.ud.policies);
          }
          angular.forEach(newPolicies, function (policy) {
            me.ud.policies.push(policy);
          });

          me.ud.id = '';
        });
      };

      this.addPolicy = function () {
        me.ud.policies.push(createPolicy());
      };

      this.deletePolicy = function (policyToDelete) {
        var index;

        angular.forEach(me.ud.policies, function (policy, idx) {
          if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write &&
                    policy.read === policyToDelete.read) {
            index = idx;
          }
        });

        if (angular.isDefined(index)) {
          me.ud.policies.splice(index, 1);
        }
      };

      this.isValid = function () {
        var hasRights = false,
            rulesValid = true;

        angular.forEach(me.ud.policies, function (policy) {
          rulesValid = false;

          if (policy.read && policy.write) {
            hasRights = true;
          }

          if ((policy.read || policy.write || policy.actions.value.length > 0) && !angular.isUndefined(policy.role)) {
            rulesValid = true;
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

      me.roles = {};

      me.getMoreRoles = function (value) {

        if (me.loading)
          return rolePromise;

        me.loading = true;
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
            me.roles[role.name] = role.value;
          });
          roleOffset = Object.keys(me.roles).length;
        }).catch(angular.noop
        ).finally(function () {
          me.loading = false;
        });
        return rolePromise;
      };

      me.getMoreRoles();

      this.reset = function () {
        me.ud = {
          id: {},
          policies: []
        };
        // Add user role after reset
        me.ud.policies = addUserRolePolicy(me.ud.policies);
      };

      this.reset();
    };
    return new Access();
  }]);
