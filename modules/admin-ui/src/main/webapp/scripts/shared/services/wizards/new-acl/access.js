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
.factory('NewAclAccess', ['ResourcesListResource', 'UserRolesResource', 'AclResource', function (ResourcesListResource,
  UserRolesResource, AclResource) {
  var Access = function () {

    var roleSlice = 100;
    var roleOffset = 0;
    var loading = false;
    var rolePromise = null;

    var me = this,
        createPolicy = function (role) {
          return {
            role  : role,
            read  : false,
            write : false,
            actions : {
              name : 'new-acl-actions',
              value : []
            }
          };
        };

    me.isAccessState = true;
    me.ud = {};
    me.ud.id = {};
    me.ud.policies = [];
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

        me.ud.policies = [];
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

    me.roles = {};

    me.getMoreRoles = function (value) {

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
          me.roles[role.name] = role.value;
        });
        roleOffset = Object.keys(me.roles).length;
      }).catch(angular.noop
      ).finally(function () {
        loading = false;
      });
      return rolePromise;
    };

    me.getMoreRoles();

    this.reset = function () {
      me.ud = {
        id: {},
        policies: []
      };
    };

    this.reset();
  };
  return new Access();
}]);
