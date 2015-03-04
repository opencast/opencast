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
.controller('SerieCtrl', ['$scope', 'SeriesMetadataResource', 'SeriesEventsResource', 'SeriesAccessResource', 'SeriesThemeResource', 'ResourcesListResource', 'Notifications',
        function ($scope, SeriesMetadataResource, SeriesEventsResource, SeriesAccessResource, SeriesThemeResource, ResourcesListResource, Notifications) {

    var saveFns = {},
        mainCatalog = 'dublincore/series', fetchChildResources,
        createPolicy = function (role) {
            return {
                role  : role,
                read  : false,
                write : false
            };
        },
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
                $scope.accessSave();
            }
        };

    $scope.policies = [];
    $scope.baseAcl = {};

    $scope.changeBaseAcl = function () {
        $scope.baseAcl = SeriesAccessResource.getManagedAcl({id: this.baseAclId}, function () {
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

        $scope.accessSave();
    };


    fetchChildResources = function (id) {
        $scope.metadata = SeriesMetadataResource.get({ id: id }, function (metadata) {
            var seriesCatalogIndex, keepGoing = true;
            angular.forEach(metadata.entries, function (catalog, index) {
                if (catalog.flavor === mainCatalog) {
                    $scope.seriesCatalog = catalog;
                    seriesCatalogIndex = index;
                    angular.forEach(catalog.fields, function (entry) {
                        if (keepGoing && entry.locked) {
                            metadata.locked = entry.locked;
                            keepGoing = false;
                        }
                    });
                }
            });

            if (angular.isDefined(seriesCatalogIndex)) {
                metadata.entries.splice(seriesCatalogIndex, 1);
            }
        });

        $scope.access = SeriesAccessResource.get({id: id}, function (data) {
            if (angular.isDefined(data.series_access)) {
                var json = angular.fromJson(data.series_access.acl);
                changePolicies(json.acl.ace, true);
            }
        });

        $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
        $scope.roles = ResourcesListResource.get({ resource: 'ROLES' });

        $scope.theme = {};

        ResourcesListResource.get({ resource: 'THEMES.NAME' }, function (data) {
            $scope.themes = data;

            //after themes have been loaded we match the current selected
            SeriesThemeResource.get({id: id}, function (response) {

                //we want to get dir of $resolved, etc. - therefore we use toJSON()
                angular.forEach(data.toJSON(), function (value, key) {

                    if (angular.isDefined(response[key])) {
                        $scope.theme.current = key;
                        return false;
                    }
                });
            });
        });

    };

      // Generate proxy function for the save metadata function based on the given flavor
      // Do not generate it
    $scope.getSaveFunction = function (flavor) {
        var fn = saveFns[flavor],
            catalog;

        if (angular.isUndefined(fn)) {
            if ($scope.seriesCatalog.flavor === flavor) {
                catalog = $scope.seriesCatalog;
            } else {
                angular.forEach($scope.metadata.entries, function (c) {
                    if (flavor === c.flavor) {
                        catalog = c;
                    }
                });
            }

            fn = function (id, callback) {
                $scope.metadataSave(id, callback, catalog);
            };

            saveFns[flavor] = fn;
        }
        return fn;
    };

    $scope.replyToId = null; // the id of the comment to which the user wants to reply

    fetchChildResources($scope.resourceId);

    $scope.$on('change', function (event, id) {
        fetchChildResources(id);
    });

    $scope.metadataSave = function (id, callback, catalog) {
        catalog.attributeToSend = id;

        SeriesMetadataResource.save({ id: $scope.resourceId }, catalog,  function () {
            if (angular.isDefined(callback)) {
                callback();
            }

            // Mark the saved attribute as saved
            angular.forEach(catalog.fields, function (entry) {
                if (entry.id === id) {
                    entry.saved = true;
                }
            });
        });
    };

    $scope.accessSave = function (field) {
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

        SeriesAccessResource.save({id: $scope.resourceId}, {
            acl: {
                ace: ace
            },
            override: true
        });
    };

    $scope.themeSave = function () {
        var theme = $scope.theme.current;

        if (angular.isUndefined(theme) || theme === null) {
            SeriesThemeResource.delete({id: $scope.resourceId}, {theme: theme}, function () {
                Notifications.add('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
            });
        } else {
            SeriesThemeResource.save({id: $scope.resourceId}, {theme: theme}, function () {
                Notifications.add('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
            });
        }
    };

}]);
