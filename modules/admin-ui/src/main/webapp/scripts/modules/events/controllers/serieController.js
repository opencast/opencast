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
.controller('SerieCtrl', ['$scope', 'SeriesMetadataResource', 'SeriesEventsResource', 'SeriesAccessResource',
  'SeriesThemeResource', 'ResourcesListResource', 'UserRolesResource', 'Notifications', 'AuthService',
  'StatisticsReusable', '$http',
  function ($scope, SeriesMetadataResource, SeriesEventsResource, SeriesAccessResource, SeriesThemeResource,
    ResourcesListResource, UserRolesResource, Notifications, AuthService, StatisticsReusable, $http) {

    var roleSlice = 100;
    var roleOffset = 0;
    var loading = false;
    var rolePromise = null;

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
            } else if (acl.allow === true || acl.allow === 'true') {
              newPolicies[acl.role].actions.value.push(acl.action);
            }
          });

          $scope.policies = [];
          angular.forEach(newPolicies, function (policy) {
            $scope.policies.push(policy);
          });

          if (loading) {
            $scope.validAcl = true;
          } else {
            $scope.accessSave();
          }
        };

    $scope.aclLocked = false,
    $scope.policies = [];
    $scope.baseAcl = {};

    AuthService.getUser().$promise.then(function (user) {
      var mode = user.org.properties['admin.series.acl.event.update.mode'];
      if (['always', 'never', 'optional'].indexOf(mode) < 0) {
        mode = 'optional'; // defaults to optional
      }
      $scope.updateMode = mode;
    }).catch(angular.noop);

    $scope.changeBaseAcl = function () {
      $scope.baseAcl = SeriesAccessResource.getManagedAcl({id: this.baseAclId}, function () {
        changePolicies($scope.baseAcl.acl.ace);
      });
      this.baseAclId = '';
    };

    $scope.addPolicy = function () {
      $scope.policies.push(createPolicy());
      $scope.validAcl = false;
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
      var previousProviderData;
      if ($scope.statReusable !== null) {
        previousProviderData = $scope.statReusable.statProviderData;
      }
      $scope.statReusable = StatisticsReusable.createReusableStatistics(
        'series',
        id,
        previousProviderData);

      $scope.metadata = SeriesMetadataResource.get({ id: id }, function (metadata) {
        var seriesCatalogIndex, keepGoing = true;
        angular.forEach(metadata.entries, function (catalog, index) {
          if (catalog.flavor === mainCatalog) {
            $scope.seriesCatalog = catalog;
            seriesCatalogIndex = index;
            var tabindex = 2;
            angular.forEach(catalog.fields, function (entry) {
              if (entry.id === 'title' && angular.isString(entry.value)) {
                $scope.titleParams = { resourceId: entry.value.substring(0,70) };
              }
              if (keepGoing && entry.locked) {
                metadata.locked = entry.locked;
                keepGoing = false;
              }
              entry.tabindex = tabindex++;
            });
          }
        });

        if (angular.isDefined(seriesCatalogIndex)) {
          metadata.entries.splice(seriesCatalogIndex, 1);
        }

        $http.get('/admin-ng/feeds/feeds')
        .then( function(response) {
          $scope.feedContent = response.data;
          for (var i = 0; i < $scope.seriesCatalog.fields.length; i++) {
            if($scope.seriesCatalog.fields[i].id === 'identifier'){
              $scope.uid = $scope.seriesCatalog.fields[i].value;
            }
          }
          for (var j = 0; j < response.data.length; j++) {
            if(response.data[j].name === 'Series') {
              var pattern = response.data[j].identifier.split('/series')[0] + response.data[j].pattern;
              var uidLink = pattern.split('<series_id>')[0] + $scope.uid;
              var typeLink = uidLink.split('<type>');
              var versionLink = typeLink[1].split('<version>');
              $scope.feedsLinks = [
                {
                  type: 'atom',
                  version: '0.3',
                  link: typeLink[0] + 'atom' + versionLink[0] + '0.3' + versionLink[1]
                },
                {
                  type: 'atom',
                  version: '1.0',
                  link: typeLink[0] + 'atom' + versionLink[0] + '1.0' + versionLink[1]
                },
                {
                  type: 'rss',
                  version: '2.0',
                  link: typeLink[0] + 'rss' + versionLink[0] + '2.0' + versionLink[1]
                }
              ];
            }
          }

        }).catch(function(error) {
          $scope.feedContent = null;
        });

      });

      $scope.roles = {};

      $scope.access = SeriesAccessResource.get({ id: id }, function (data) {
        if (angular.isDefined(data.series_access)) {
          var json = angular.fromJson(data.series_access.acl);
          changePolicies(json.acl.ace, true);

          $scope.aclLocked = data.series_access.locked;

          if ($scope.aclLocked) {
            aclNotification = Notifications.add('warning', 'SERIES_ACL_LOCKED', 'series-acl-' + id, -1);
          } else if (aclNotification) {
            Notifications.remove(aclNotification, 'series-acl');
          }
          angular.forEach(data.series_access.privileges, function(value, key) {
            if (angular.isUndefined($scope.roles[key])) {
              $scope.roles[key] = key;
            }
          });
        }
      });

      $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
      $scope.actions = {};
      $scope.hasActions = false;
      ResourcesListResource.get({ resource: 'ACL.ACTIONS' }, function(data) {
        angular.forEach(data, function (value, key) {
          if (key.charAt(0) !== '$') {
            $scope.actions[key] = value;
            $scope.hasActions = true;
          }
        });
      });
      $scope.aclLocked = false,

      $scope.selectedTheme = {};

      $scope.updateSelectedThemeDescripton = function () {
        if(angular.isDefined($scope.themeDescriptions)) {
          $scope.selectedTheme.description = $scope.themeDescriptions[$scope.selectedTheme.id];
        }
      };

      ResourcesListResource.get({ resource: 'THEMES.NAME' }, function (data) {
        $scope.themes = data;

        //after themes have been loaded we match the current selected
        SeriesThemeResource.get({ id: id }, function (response) {

          //we want to get rid of $resolved, etc. - therefore we use toJSON()
          angular.forEach(data.toJSON(), function (value, key) {

            if (angular.isDefined(response[key])) {
              $scope.selectedTheme.id = key;
              return false;
            }
          });

          ResourcesListResource.get({ resource: 'THEMES.DESCRIPTION' }, function (data) {
            $scope.themeDescriptions = data;
            $scope.updateSelectedThemeDescripton();
          });
        });
      });

      $scope.getMoreRoles();
    };

    $scope.statReusable = null;

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

    $scope.statisticsCsvFileName = function (statsTitle) {
      var sanitizedStatsTitle = statsTitle.replace(/[^0-9a-z]/gi, '_').toLowerCase();
      return 'export_series_' + $scope.resourceId + '_' + sanitizedStatsTitle + '.csv';
    };

    $scope.metadataSave = function (id, callback, catalog) {
      catalog.attributeToSend = id;

      if (Object.prototype.hasOwnProperty.call(catalog, 'fields')) {
        for (var fieldNo in catalog.fields) {
          var field = catalog.fields[fieldNo];
          if (Object.prototype.hasOwnProperty.call(field, 'collection')) {
            field.collection = [];
          }
        }
      }

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

    $scope.accessChanged = function (role) {
      if (role) {
        $scope.accessSave();
      }
    };

    $scope.accessSave = function (override) {
      var ace = [],
          hasRights = false,
          rulesValid = false;

      $scope.validAcl = false;
      override = override === true || $scope.updateMode === 'always';

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

          angular.forEach(policy.actions.value, function(customAction) {
            ace.push({
              'action' : customAction,
              'allow'  : true,
              'role'   : policy.role
            });
          });
        }
      });

      $scope.validAcl = rulesValid;
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
        SeriesAccessResource.save({ id: $scope.resourceId }, {
          acl: {
            ace: ace
          },
          override: override
        });

        Notifications.add('info', 'SAVED_ACL_RULES', NOTIFICATION_CONTEXT, 1200);
      }
    };

    // Reload tab resource on tab changes
    $scope.$parent.$watch('tab', function (value) {
      switch (value) {
      case 'permissions':
        $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
        $scope.getMoreRoles();
        break;
      }
    });

    $scope.themeSave = function () {
      var selectedThemeID = $scope.selectedTheme.id;
      $scope.updateSelectedThemeDescripton();

      if (angular.isUndefined(selectedThemeID) || selectedThemeID === null) {
        SeriesThemeResource.delete({ id: $scope.resourceId }, { theme: selectedThemeID }, function () {
          Notifications.add('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
        });
      } else {
        SeriesThemeResource.save({ id: $scope.resourceId }, { theme: selectedThemeID }, function () {
          Notifications.add('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
        });
      }
    };

  }]);
