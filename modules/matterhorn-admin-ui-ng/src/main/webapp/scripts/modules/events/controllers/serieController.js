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
.controller('SerieCtrl', ['$scope', '$q', 'SeriesMetadataResource', 'SeriesEventsResource', 'SeriesAccessResource', 'SeriesThemeResource', 'ResourcesListResource', 'Notifications',
        'OptoutSingleResource', 'SeriesParticipationResource',
        function ($scope, $q, SeriesMetadataResource, SeriesEventsResource, SeriesAccessResource, SeriesThemeResource, ResourcesListResource, Notifications,
            OptoutSingleResource, SeriesParticipationResource) {

    var saveFns = {}, aclNotification,
        me = this,
        NOTIFICATION_CONTEXT = 'series-acl',
        mainCatalog = 'dublincore/series', fetchChildResources,
        createPolicy = function (role) {
            return {
                role  : role,
                read  : false,
                write : false,
                actions : {
                    name : 'series-acl-actions',
                    value : []
                }
            };
        },
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
                $scope.accessSave();
            }
        },
        updateRoles = function() {
            //MH-11716: We have to wait for both the access (series ACL), and the roles (list of system roles)
            //to resolve before we can add the roles that are present in the series but not in the system
            ResourcesListResource.get({ resource: 'ROLES' }, function (results) {
                var roles = results;
                angular.forEach($scope.access.series_access.privileges, function(value, key) {
                    if (angular.isUndefined(roles[key])) {
                        roles[key] = key;
                    }
                }, this);
                $scope.roles = roles;
            }, this);
        };

    $scope.aclLocked = false,
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

    $scope.updateOptout = function (newBoolean) {

        OptoutSingleResource.save({
            resource: 'series',
            id: $scope.resourceId,
            optout: newBoolean
        }, function () {
            Notifications.add('success', 'SERIES_PARTICIPATION_STATUS_UPDATE_SUCCESS', 'series-participation');
        }, function () {
            Notifications.add('error', 'SERIES_PARTICIPATION_STATUS_UPDATE_ERROR', 'series-participation');
        });
    };


    fetchChildResources = function (id) {
        $scope.metadata = SeriesMetadataResource.get({ id: id }, function (metadata) {
            var seriesCatalogIndex, keepGoing = true;
            angular.forEach(metadata.entries, function (catalog, index) {
                if (catalog.flavor === mainCatalog) {
                    $scope.seriesCatalog = catalog;
                    seriesCatalogIndex = index;
                    angular.forEach(catalog.fields, function (entry) {
                        if (entry.id === 'title' && angular.isString(entry.value)) {
                            $scope.titleParams = { resourceId: entry.value.substring(0,70) };
                        }
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

                $scope.aclLocked = data.series_access.locked;

                if ($scope.aclLocked) {
                    aclNotification = Notifications.add('warning', 'SERIES_ACL_LOCKED', 'series-acl-' + id, -1);
                } else if (aclNotification) {
                    Notifications.remove(aclNotification, 'series-acl');
                }

            }

        });

        $scope.participation = SeriesParticipationResource.get({ id: id });
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

        updateRoles();
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

    $scope.accessSave = function () {
            var ace = [],
                hasRights = false,
                rulesValid = false;

            angular.forEach($scope.policies, function (policy) {
                rulesValid = false;

                if (policy.read && policy.write) {
                    hasRights = true;
                }

                if ((policy.read || policy.write || policy.actions.value.length > 0) && !angular.isUndefined(policy.role)) {
                    rulesValid = true;

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

            me.unvalidRule = !rulesValid;
            me.hasRights = hasRights;

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

            if (hasRights && rulesValid) {
                SeriesAccessResource.save({id: $scope.resourceId}, {
                    acl: {
                        ace: ace
                    },
                    override: true
                });

                Notifications.add('info', 'SAVED_ACL_RULES', NOTIFICATION_CONTEXT, 1200);
            }
    };

    // Reload tab resource on tab changes
    $scope.$parent.$watch('tab', function (value) {
      switch (value) {
        case 'permissions':
            $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
            updateRoles();
          break;
      }
    });

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
