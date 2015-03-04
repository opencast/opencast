/**
* Copyright 2009-2013 The Regents of the University of California
* Licensed under the Educational Community License, Version 2.0
* (the "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.osedu.org/licenses/ECL-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an "AS IS"
* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*
*/
'use strict';

// Controller for all single series screens.
angular.module('adminNg.controllers')
.controller('AclCtrl', ['$scope', 'AclResource', 'ResourcesListResource', 'Notifications',
        function ($scope, AclResource, ResourcesListResource, Notifications) {

    var createPolicy = function (role) {
            return {
                role  : role,
                read  : false,
                write : false
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
                newPolicies[acl.role][acl.action] = acl.allow;
            });

            $scope.policies = [];
            angular.forEach(newPolicies, function (policy) {
                $scope.policies.push(policy);
            });

            if (!loading) {
                $scope.saveAcl();
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

        $scope.saveAcl();
    };


    fetchChildResources = function (id) {
        AclResource.get({id: id}, function (data) {
            $scope.metadata.name = data.name;

            if (angular.isDefined(data.acl)) {
                var json = angular.fromJson(data.acl);
                changePolicies(json.ace, true);
            }
        });

        $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
        $scope.roles = ResourcesListResource.get({ resource: 'ROLES' });
    };


    fetchChildResources($scope.resourceId);

    $scope.$on('change', function (event, id) {
        fetchChildResources(id);
    });

    $scope.save = function (field) {
        var ace = [];

        if (angular.isDefined(field) && angular.isUndefined(field.role)) {
            return;
        }

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
            }

        });

        AclResource.save({id: $scope.resourceId}, {
            acl: {
                ace: ace
            },
            name: $scope.metadata.name
        }, function () {
            Notifications.add('success', 'ACL_UPDATED', 'acl-form');
        }, function () {
            Notifications.add('error', 'ACL_NOT_SAVED', 'acl-form');
        });
    };
}]);
