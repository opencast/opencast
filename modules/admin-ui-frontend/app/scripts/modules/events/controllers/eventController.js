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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('EventCtrl', [
  '$scope', 'Notifications', 'EventTransactionResource', 'EventMetadataResource', 'EventAssetsResource',
  'EventAssetCatalogsResource', 'CommentResource', 'EventWorkflowsResource', 'EventWorkflowActionResource',
  'EventWorkflowDetailsResource', 'ResourcesListResource', 'RolesResource', 'EventAccessResource',
  'EventPublicationsResource', 'EventSchedulingResource','NewEventProcessingResource', 'CaptureAgentsResource',
  'ConflictCheckResource', 'Language', 'JsHelper', '$sce', '$timeout', 'EventHelperService', 'UploadAssetOptions',
  'EventUploadAssetResource', 'Table', 'SchedulingHelperService', 'StatisticsReusable', 'Modal', '$translate',
  'MetadataSaveService', 'UserResource', 'UsersResource',
  function ($scope, Notifications, EventTransactionResource, EventMetadataResource, EventAssetsResource,
    EventAssetCatalogsResource, CommentResource, EventWorkflowsResource, EventWorkflowActionResource,
    EventWorkflowDetailsResource, ResourcesListResource, RolesResource, EventAccessResource,
    EventPublicationsResource, EventSchedulingResource, NewEventProcessingResource, CaptureAgentsResource,
    ConflictCheckResource, Language, JsHelper, $sce, $timeout, EventHelperService, UploadAssetOptions,
    EventUploadAssetResource, Table, SchedulingHelperService, StatisticsReusable, Modal, $translate,
    MetadataSaveService, UserResource, UsersResource) {

    var metadataChangedFns = {},
        me = this,
        NOTIFICATION_CONTEXT = 'events-access',
        SCHEDULING_CONTEXT = 'event-scheduling',
        mainCatalog = 'dublincore/episode',
        idConfigElement = '#event-workflow-configuration',
        workflowConfigEl = angular.element(idConfigElement),
        baseWorkflow,
        createPolicy = function (role, read, write, actionValues) {
          return {
            role  : role,
            read  : read !== undefined ? read : false,
            write : write !== undefined ? write : false,
            actions : {
              name : 'event-acl-actions',
              value : actionValues !== undefined ? actionValues : [],
            },
            user: undefined,
          };
        },
        findWorkflow = function (id) {
          var workflow;

          angular.forEach($scope.workflowDefinitions, function (w) {
            if (w.id === id) {
              workflow = w;
            }
          });

          if (!angular.isDefined(workflow)) {
            return baseWorkflow;
          } else {
            return workflow;
          }

        },
        addChangeDetectionToInputs = function () {
          var element, isRendered = angular.element(idConfigElement).find('.configField').length > 0;
          if (!angular.isDefined($scope.workflows.workflow.configuration_panel)
            || !$scope.workflows.workflow.configuration_panel.trim()) {
            // The workflow contains no configuration (it is empty), therefore it is rendered.
            isRendered = true;
          }
          if (!isRendered) {
            $timeout(addChangeDetectionToInputs, 200);
            return;
          } else {
            element = angular.element(idConfigElement).find('.configField');
          }

          element.each(function (idx, el) {
            var element = angular.element(el);

            if (angular.isDefined(element.attr('id'))) {
              element.change($scope.saveWorkflowConfig);
            }
          });
        },
        updateConfigurationPanel = function (html) {
          if (angular.isUndefined(html)) {
            html = undefined;
          }
          $scope.workflowConfiguration = $sce.trustAsHtml(html);
          addChangeDetectionToInputs();
        },
        // Get the workflow configuration
        getWorkflowConfig = function () {
          var workflowConfig = {},
              element,
              isRendered = angular.element(idConfigElement).find('.configField').length > 0;

          if (!angular.isDefined($scope.workflows.workflow.configuration_panel)
            || !$scope.workflows.workflow.configuration_panel.trim()) {
            // The workflow contains no configuration (it is empty), therefore it is rendered.
            isRendered = true;
          }

          if (!isRendered) {
            element = angular.element($scope.workflows.workflow.configuration_panel).find('.configField');
          } else {
            element = angular.element(idConfigElement).find('.configField');
          }

          element.each(function (idx, el) {
            var element = angular.element(el);

            if (angular.isDefined(element.attr('id'))) {
              if (element.is('[type=checkbox]') || element.is('[type=radio]')) {
                workflowConfig[element.attr('id')] = element.is(':checked') ? 'true' : 'false';
              } else {
                workflowConfig[element.attr('id')] = element.val();
              }
            }
          });

          return workflowConfig;
        },
        setWorkflowConfig = function () {
          var isRendered = angular.element(idConfigElement).find('.configField').length > 0;

          if (!angular.isDefined($scope.workflows.workflow.configuration_panel)
            || !$scope.workflows.workflow.configuration_panel.trim()) {
            // The workflow contains no configuration (it is empty), therefore it is rendered.
            isRendered = true;
          }

          if (!isRendered) {
            $timeout(setWorkflowConfig, 200);
          } else {
            angular.forEach(baseWorkflow.configuration, function (value, key) {
              var el = angular.element(idConfigElement).find('#' + key + '.configField');

              if (el.length > 0) {
                if (el.is('[type=checkbox]') || el.is('[type=radio]')) {
                  if (value === 'true' || value === true) {
                    el.attr('checked', 'checked');
                  } else {
                    el.removeAttr('checked');
                  }
                } else {
                  el.val(value);
                }

                // trigger any UI changes
                el.trigger('updateConfigUI');
              }

            });
            me.loadingWorkflow = false;
          }
        },
        changePolicies = function (access, loading) {
          var newPolicies = {};
          if (!Array.isArray(access)) {
            access = [access];
          }
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

          // add policy to allow ROLE_USER_* to read and write
          var userRole = Object.keys($scope.roles).filter(function(role){
            return role.startsWith($scope.roleUserPrefix) && role != 'ROLE_USER_ADMIN';
          });
          if (angular.isDefined(userRole) && userRole.length == 1){
            userRole = userRole[0];
            if (angular.isUndefined(newPolicies[userRole])){
              newPolicies[userRole] = createPolicy(userRole);
            }
            newPolicies[userRole]['read'] = true;
            newPolicies[userRole]['write'] = true;
          }

          $scope.policies = [];
          angular.forEach(newPolicies, function (policy) {
            if (!policy.role.startsWith($scope.roleUserPrefix)) {
              $scope.policies.push(policy);
            }
          });

          $scope.policiesUser = [];
          angular.forEach(newPolicies, function (policy) {
            if (policy.role.startsWith($scope.roleUserPrefix)) {
              var id = policy.role.split($scope.roleUserPrefix).pop();
              // FIXMe: If roles are sanitized, WE CANNOT derive the user from their user role,
              //  because the user roles are converted to uppercase, while usernames are case sensitive.
              //  This is a terrible workaround, because usernames are usually all lowercase anyway.
              if ($scope.aclCreateDefaults['sanitize']) {
                id = id.toLowerCase();
              }

              UserResource.get({ username: id }).$promise.then(function (data) {
                policy.user = data;
              }).catch(function() {
                policy.userDoesNotExist = id;
              });

              $scope.policiesUser.push(policy);
            }
          });
        },
        checkForActiveTransactions = function () {
          EventTransactionResource.hasActiveTransaction({id: $scope.resourceId }, function (data) {
            $scope.transactions.read_only = angular.isUndefined(data.hasActiveTransaction)
              ? true
              : data.hasActiveTransaction;

            if ($scope.transactions.read_only) {
              if (!angular.isUndefined(me.transactionNotification)) {
                Notifications.remove(me.transactionNotification, NOTIFICATION_CONTEXT);
              }
              me.transactionNotification = Notifications.add('warning', 'ACTIVE_TRANSACTION', NOTIFICATION_CONTEXT);
              $scope.$emit('ACTIVE_TRANSACTION');
            } else {
              if (!angular.isUndefined(me.transactionNotification)) {
                Notifications.remove(me.transactionNotification, NOTIFICATION_CONTEXT);
              }
              $scope.$emit('NO_ACTIVE_TRANSACTION');
            }
          });

          $scope.checkForActiveTransactionsTimer = $timeout(checkForActiveTransactions, 3000);
        },
        cleanupScopeResources = function() {
          $timeout.cancel($scope.checkForActiveTransactionsTimer);
          if ($scope.lastNotificationId) {
            Notifications.remove($scope.lastNotificationId, 'event-scheduling');
            $scope.lastNotificationId = undefined;
          }
          me.clearConflicts();
        },
        fetchChildResources = function (id) {
          var previousProviderData;
          if ($scope.statReusable !== null) {
            previousProviderData = $scope.statReusable.statProviderData;
          }
          $scope.statReusable = StatisticsReusable.createReusableStatistics(
            'episode',
            id,
            previousProviderData);

          var publications = EventPublicationsResource.get({ id: id }, function () {
            angular.forEach(publications.publications, function (publication, index) {
              publication.label = publication.name;
              publication.order = 999 + index;
              var now = new Date();
              if (publication.id == 'engage-live' &&
                (now < new Date(publications['start-date']) || now > new Date(publications['end-date'])))
                publication.enabled = false;
              else publication.enabled = true;
            });
            $scope.publicationChannels = ResourcesListResource.get({ resource: 'PUBLICATION.CHANNELS' }, function() {
              angular.forEach(publications.publications, function (publication) {
                if(angular.isDefined($scope.publicationChannels[publication.id])) {
                  var record = JSON.parse($scope.publicationChannels[publication.id]);
                  if (record.label) publication.label = record.label;
                  if (record.icon) publication.icon = record.icon;
                  if (record.hide) publication.hide = record.hide;
                  if (record.description) publication.description = record.description;
                  if (record.order) publication.order = record.order;
                }
              });
              // we postpone setting $scope.publications until this point to avoid UI "flickering" due to publications
              // changing
              $scope.publications = publications;
            }, function() {
              $scope.publications = publications;
            });
          });

          $scope.metadata =  EventMetadataResource.get({ id: id }, function (metadata) {
            $scope.extendedMetadataCatalogs = [];
            angular.forEach(metadata.entries, function (catalog, index) {
              // common metadata
              if (catalog.flavor === mainCatalog) {
                $scope.commonMetadataCatalog = catalog;
              // extended metadata
              } else {
                $scope.extendedMetadataCatalogs.push(catalog);
              }

              // hook up tabindex
              var tabindex = 2;
              angular.forEach(catalog.fields, function (entry) {
                entry.tabindex = tabindex ++;
                // find title
                if (catalog.flavor === mainCatalog && entry.id === 'title' && angular.isString(entry.value)) {
                  $scope.titleParams = { resourceId: entry.value.substring(0,70) };
                }
                // metadata locked?
                if (entry.locked) {
                  metadata.locked = entry.locked;
                }
                // save original values
                if (entry.value instanceof Array) {
                  entry.oldValue = entry.value.slice(0);
                } else {
                  entry.oldValue = entry.value;
                }
              });
            });

          });

          //<===============================
          // Enable asset upload (catalogs, attachments and tracks) to existing events

          // Retrieve option configuration for asset upload
          UploadAssetOptions.getOptionsPromise().then(function(data){
            if (data) {
              $scope.assetUploadWorkflowDefId = data.workflow;
              $scope.uploadAssetOptions = data.options;
              // if no asset options, undefine the option variable
              $scope.uploadAssetOptions = $scope.uploadAssetOptions.length > 0 ? $scope.uploadAssetOptions : undefined;
              $scope.newAssets = {};
            }
          });
          $scope.saveAssetsKeyUp = function (event) {
            if (event.keyCode === 13 || event.keyCode === 32) {
              $scope.saveAssets();
            }
          };

          // Save and start upload asset request and workflow
          $scope.saveAssets = function() {
            // The transaction becomes read-only if a workflow is running for this event.
            // Ref endpoint hasActiveTransaction(@PathParam("eventId") String eventId)
            if ($scope.transactions.read_only) {
              me.transactionNotification = Notifications.add('warning', 'ACTIVE_TRANSACTION', NOTIFICATION_CONTEXT,
                3000);
              return;
            }
            // Verify there are assets to upload
            if (angular.equals($scope.newAssets, {})) {
              return;
            }
            var userdata = { metadata: {}};

            // save metadata map (contains flavor mapping used by the server)
            userdata.metadata['assets'] = ($scope.uploadAssetOptions);

            // save file assets (passed in a separate request field from its metadata map)
            userdata['upload-asset'] = $scope.newAssets;

            // save workflow definition id (defined in the asset upload configuration provided-list)
            userdata['workflow'] = $scope.assetUploadWorkflowDefId;

            EventUploadAssetResource.save({id: $scope.resourceId }, userdata, function (data) {
              me.transactionNotification = Notifications.add('success', 'EVENTS_CREATED', NOTIFICATION_CONTEXT, 6000);
              $scope.openTab('assets');
            }, function () {
              me.transactionNotification = Notifications.add('error', 'EVENTS_NOT_CREATED', NOTIFICATION_CONTEXT, 6000);
              $scope.openTab('assets');
            });
          };
          // <==========================

          $scope.acls = ResourcesListResource.get({ resource: 'ACL' });
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
            $scope.aclCreateDefaults['default_actions'] = $scope.aclCreateDefaults['default_actions'] !== undefined
              ? $scope.aclCreateDefaults['default_actions'].split(',') : [];
            $scope.roleUserPrefix = $scope.aclCreateDefaults['role_user_prefix'] !== undefined
              ? $scope.aclCreateDefaults['role_user_prefix']
              : 'ROLE_USER_';
            $scope.aclCreateDefaults['sanitize'] = $scope.aclCreateDefaults['sanitize'] !== undefined
              ? ($scope.aclCreateDefaults['sanitize'].toLowerCase() === 'true') : true;
            $scope.aclCreateDefaults['keep_on_template_switch_role_prefixes'] =
              $scope.aclCreateDefaults['keep_on_template_switch_role_prefixes'] !== undefined
                ? $scope.aclCreateDefaults['keep_on_template_switch_role_prefixes'].split(',') : [];
          });

          $scope.assets = EventAssetsResource.get({ id: id });

          $scope.setWorkflowDefinitions = function (workflowDefinitions) {
            $scope.workflowDefinitions = workflowDefinitions;
            $scope.workflowDefinitionIds = workflowDefinitions.map(function (w) {return w.id;});
          };

          $scope.workflow = {};
          $scope.workflows = EventWorkflowsResource.get({ id: id }, function () {
            if (angular.isDefined($scope.workflows.workflow)) {
              baseWorkflow = $scope.workflows.workflow;
              $scope.workflow.id = $scope.workflows.workflow.workflowId;
              $scope.workflowDefinitionsObject = NewEventProcessingResource.get({
                tags: 'schedule'
              }, function () {
                $scope.setWorkflowDefinitions($scope.workflowDefinitionsObject.workflows);
                $scope.changeWorkflow(true);
                setWorkflowConfig();
              });
            }
          });

          $scope.source = EventSchedulingResource.get({ id: id }, function (source) {
            source.presenters = angular.isArray(source.presenters) ? source.presenters.join(', ') : '';
            $scope.scheduling.hasProperties = true;
            CaptureAgentsResource.query({inputs: true}).$promise.then(function (data) {
              $scope.captureAgents = data.rows;
              angular.forEach(data.rows, function (agent) {
                var inputs;
                if (agent.id === $scope.source.agentId) {
                  source.device = agent;
                  // Retrieve agent inputs configuration
                  if (angular.isDefined(source.agentConfiguration['capture.device.names'])) {
                    inputs = source.agentConfiguration['capture.device.names'].split(',');
                    source.device.inputMethods = {};
                    angular.forEach(inputs, function (input) {
                      source.device.inputMethods[input] = true;
                    });
                  }
                }
              });
            }).catch(angular.noop);
          }, function () {
            $scope.scheduling.hasProperties = false;
          });

          $scope.access = EventAccessResource.get({ id: id }, function (data) {
            if (angular.isDefined(data.episode_access)) {
              $scope.baseAclId = data.episode_access.current_acl.toString();
              var json = angular.fromJson(data.episode_access.acl);
              $scope.aclCreateDefaults.$promise.then(function () { // needed for roleUserPrefix
                changePolicies(json.acl.ace, true);
                getCurrentPolicies();
              });
            }
          });

          $scope.roles = RolesResource.queryNameOnly({limit: -1, target:'ACL' });

          //MH-11716: We have to wait for both the access (event ACL), and the roles (list of system roles)
          //to resolve before we can add the roles that are present in the event but not in the system
          $scope.access.$promise.then(function () {
            $scope.roles.$promise.then(function() {
              angular.forEach(Object.keys($scope.access.episode_access.privileges), function(newRole) {
                if ($scope.roles.indexOf(newRole) == -1) {
                  $scope.roles.push(newRole);
                }
              });
            });
          });

          $scope.comments = CommentResource.query({ resource: 'event', resourceId: id, type: 'comments' });

          $scope.users = UsersResource.query({limit: 2147483647});
          $scope.users.$promise.then(function () {
            $scope.aclCreateDefaults.$promise.then(function () {
              var newUsers = [];
              angular.forEach($scope.users.rows, function(user) {
                if ($scope.aclCreateDefaults['sanitize']) {
                  // FixMe: See the FixMe above pertaining to sanitize
                  user.userRole = $scope.roleUserPrefix + user.username.replace(/\W/g, '_').toUpperCase();
                } else {
                  user.userRole = $scope.roleUserPrefix + user.username;
                }
                newUsers.push(user);
              });
              $scope.users = newUsers;
            });
          });
        },
        tzOffset = (new Date()).getTimezoneOffset() / -60;

    $scope.statReusable = null;

    $scope.getMatchingRoles = function (value) {
      RolesResource.queryNameOnly({query: value, target: 'ACL'}).$promise.then(function (data) {
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

    /**
     * <===============================
     * START Scheduling related resources
     */

    /* Get the current client timezone */
    $scope.tz = 'UTC' + (tzOffset < 0 ? '-' : '+') + tzOffset;

    $scope.scheduling = {};
    $scope.sortedWeekdays = JsHelper.getWeekDays();
    $scope.hours = JsHelper.initArray(24);
    $scope.minutes = JsHelper.initArray(60);

    $scope.conflicts = [];

    this.readyToPollConflicts = function () {
      var data = $scope.source;
      return angular.isDefined(data) &&
                angular.isDefined(data.start) && angular.isDefined(data.start.date) && data.start.date.length > 0 &&
                angular.isDefined(data.duration) &&
                angular.isDefined(data.duration.hour) && angular.isDefined(data.duration.minute) &&
                angular.isDefined(data.device) && angular.isDefined(data.device.id) && data.device.id.length > 0;
    };

    this.clearConflicts = function () {
      $scope.conflicts = [];
      if (me.notificationConflict) {
        Notifications.remove(me.notificationConflict, SCHEDULING_CONTEXT);
        me.notificationConflict = undefined;
      }
      if (me.inThePastNotification) {
        Notifications.remove(me.inThePastNotification, SCHEDULING_CONTEXT);
        me.inThePastNotification = undefined;
      }
    };

    this.noConflictsDetected = function () {
      me.clearConflicts();
      $scope.checkingConflicts = false;
    };

    this.conflictsDetected = function (response) {
      me.clearConflicts();
      if (response.status === 409) {
        me.notificationConflict = Notifications.add('error', 'CONFLICT_DETECTED', SCHEDULING_CONTEXT);
        angular.forEach(response.data, function (data) {
          $scope.conflicts.push({
            title: data.title,
            start: Language.formatDateTime('medium', data.start),
            end: Language.formatDateTime('medium', data.end)
          });
        });
      }
      $scope.checkingConflicts = false;
    };

    this.checkValidity = function () {
      // check if start is in the past
      if (SchedulingHelperService.alreadyEnded($scope.source.start, $scope.source.duration)) {
        me.inThePastNotification = Notifications.add('error', 'CONFLICT_IN_THE_PAST',
          SCHEDULING_CONTEXT, -1);
        $scope.checkingConflicts = false;
        return false;
      }

      return true;
    };

    $scope.checkConflicts = function () {
      return new Promise(function(resolve, reject) {
        $scope.checkingConflicts = true;
        if (me.readyToPollConflicts()) {
          if (!me.checkValidity()) {
            resolve();
          } else {
            ConflictCheckResource.check($scope.source, me.noConflictsDetected, me.conflictsDetected)
              .$promise.then(function() {
                resolve();
              }).catch(function(err) {
                reject();
              });
          }
        } else {
          $scope.checkingConflicts = false;
          resolve();
        }
      });
    };

    $scope.saveScheduling = function (newObj, oldObj) {
      if (me.readyToPollConflicts()) {
        if (!me.checkValidity()) {
          return;
        }

        ConflictCheckResource.check($scope.source, function () {
          me.clearConflicts();

          $scope.source.agentId = $scope.source.device.id;
          $scope.source.agentConfiguration['capture.device.names'] = '';

          angular.forEach($scope.source.device.inputMethods, function (value, key) {
            if (value) {
              if ($scope.source.agentConfiguration['capture.device.names'] !== '') {
                $scope.source.agentConfiguration['capture.device.names'] += ',';
              }
              $scope.source.agentConfiguration['capture.device.names'] += key;
            }
          });

          EventSchedulingResource.save({
            id: $scope.resourceId,
            entries: $scope.source,
            previousId: oldObj ? oldObj.id : undefined,
            previousEntries: oldObj? oldObj.inputMethods : undefined
          }, function () {
            fetchChildResources($scope.resourceId);
          });

          // Getting the update may take several seconds on large installations
          // Fill in input channel values if possible
          if (oldObj && $scope.source.device.inputs && oldObj.inputs) {
            var sourceInputs = $scope.source.device.inputs.map(function(input){
              return input.id;
            }).join(",");
            var oldObjInputs = oldObj.inputs.map(function(input){
              return input.id;
            }).join(",");
            if (sourceInputs === oldObjInputs) {
              $scope.source.device.inputMethods = oldObj.inputMethods;
            }
          }
        }, me.conflictsDetected);
      }
    };

    $scope.onTemporalValueChange = function(type) {
      SchedulingHelperService.applyTemporalValueChange($scope.source, type, true);
      $scope.saveScheduling();
    };


    $scope.hasCurrentAgentAccess = function() {
      return SchedulingHelperService.hasAgentAccess($scope.source.agentId);
    };

    $scope.currentAgentOrAccess = function(agent, index, array) {
      return agent.id === $scope.source.agentId || SchedulingHelperService.hasAgentAccess(agent.id);
    };

    /**
         * End Scheduling related resources
         * ===============================>
         */

    $scope.policies = [];
    $scope.policiesUser = [];
    $scope.baseAcl = {};
    $scope.baseAclId = '';

    $scope.not = function(func) {
      return function (item) {
        return !func(item);
      };
    };

    $scope.userExists = function (policy) {
      if (policy.userDoesNotExist === undefined) {
        return true;
      }
      return false;
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

    $scope.changeBaseAcl = function (id) {
      // Get the policies which should persist on template change
      var allPolicies = $scope.getAllPolicies();
      var remainingPolicies = allPolicies.filter(policy => (
        $scope.aclCreateDefaults['keep_on_template_switch_role_prefixes'].some(
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

      $scope.baseAcl = EventAccessResource.getManagedAcl({id: id}, function () {
        var combine = $scope.baseAcl.acl.ace.concat(remainingACEs);
        $scope.aclCreateDefaults.$promise.then(function () { // needed for roleUserPrefix
          changePolicies(combine);
        });
      });
    };

    // E.g. model === $scope.policies
    $scope.addPolicy = function (model) {
      model.push(createPolicy(
        undefined,
        $scope.aclCreateDefaults['read_enabled'],
        $scope.aclCreateDefaults['write_enabled'],
        $scope.aclCreateDefaults['default_actions']
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
    };

    $scope.getPreview = function (url) {
      return [{
        uri: url
      }];
    };

    me.loadingWorkflow = true;

    // Listener for the workflow selection
    $scope.changeWorkflow = function (noSave) {
      // Skip the changing workflow call if the view is not loaded
      if (me.loadingWorkflow && !noSave) {
        return;
      }

      me.changingWorkflow = true;
      workflowConfigEl = angular.element(idConfigElement);
      if (angular.isDefined($scope.workflow.id)) {
        $scope.workflows.workflow = findWorkflow($scope.workflow.id);
        updateConfigurationPanel($scope.workflows.workflow.configuration_panel);
      } else {
        updateConfigurationPanel();
      }

      if (!noSave) {
        $scope.saveWorkflowConfig();
      }
      me.changingWorkflow = false;
    };

    $scope.saveWorkflowConfig = function () {
      EventWorkflowsResource.save({
        id: $scope.resourceId,
        entries: {
          id: $scope.workflows.workflow.id,
          configuration: getWorkflowConfig()
        }
      }, function () {
        baseWorkflow = {
          workflowId: $scope.workflows.workflow.id,
          configuration: getWorkflowConfig()
        };
      });
    };

    $scope.replyToId = null; // the id of the comment to which the user wants to reply
    if (! $scope.resourceId) {
      $scope.resourceId = EventHelperService.eventId;
    }
    $scope.title = $scope.resourceId; // if nothing else use the resourceId

    fetchChildResources($scope.resourceId);

    $scope.$on('change', function (event, id) {
      cleanupScopeResources();
      fetchChildResources(id);
      checkForActiveTransactions();
    });

    $scope.transactions = {
      read_only: false
    };

    // Generate proxy function for the save metadata function based on the given flavor
    // Do not generate it
    $scope.getMetadataChangedFunction = function (flavor) {
      var fn = metadataChangedFns[flavor];
      var catalog;

      if (angular.isUndefined(fn)) {
        angular.forEach($scope.metadata.entries, function (c) {
          if (flavor === c.flavor) {
            catalog = c;
          }
        });

        fn = function (id, callback) {
          $scope.metadataChanged(id, callback, catalog);
        };

        metadataChangedFns[flavor] = fn;
      }
      return fn;
    };

    $translate('CONFIRMATIONS.WARNINGS.UNSAVED_CHANGES').then(function (translation) {
      window.unloadConfirmMsg = translation;
    }).catch(angular.noop);

    var confirmUnsaved = function() {
      // eslint-disable-next-line
      return confirm(window.unloadConfirmMsg);
    };

    $scope.close = function() {
      if (($scope.unsavedChanges([$scope.commonMetadataCatalog]) === false
           && $scope.unsavedChanges($scope.extendedMetadataCatalogs)  === false
           && unsavedAccessChanges() === false)
          || confirmUnsaved()) {
        Modal.$scope.close();
      }
    };

    $scope.unsavedChanges = function(catalogs) {
      if (angular.isDefined(catalogs)) {
        return catalogs.some(function(catalog) {
          if (angular.isDefined(catalog)) {
            return catalog.fields.some(function(field) {
              return field.dirty === true;
            });
          }
          return false;
        });
      }
      return false;
    };

    $scope.metadataChanged = function (id, callback, catalog) {
      // Mark the saved attribute as dirty
      angular.forEach(catalog.fields, function (entry) {
        if (entry.id === id) {
          if (differentValue(entry)) {
            entry.dirty = true;
          } else {
            entry.dirty = false;
          }
        }
      });

      if (angular.isDefined(callback)) {
        callback();
      }
    };

    var differentValue = function(entry) {
      if (!entry.value && !entry.oldValue) {
        return false;
      }

      if ((!entry.value && entry.oldValue) || (entry.value && !entry.oldValue)) {
        return true;
      }

      if (entry.value instanceof Array) {
        if (entry.value.length != entry.oldValue.length) {
          return true;
        }
        for (var i = 0; i < entry.value.length; i++) {
          if (entry.value[i] !== entry.oldValue[i]) {
            return true;
          }
        }
        return false;
      } else {
        return (entry.value !== entry.oldValue);
      }
    };

    $scope.metadataSave = function (catalogs) {
      return MetadataSaveService.save(
        catalogs,
        $scope.commonMetadataCatalog,
        $scope.resourceId);
    };

    $scope.components = ResourcesListResource.get({ resource: 'components' });

    $scope.myComment = {};

    $scope.replyTo = function (comment) {
      $scope.replyToId = comment.id;
      $scope.originalComment = comment;
      $scope.myComment.resolved = false;
    };

    $scope.exitReplyMode = function () {
      $scope.replyToId = null;
      $scope.myComment.text = '';
    };

    $scope.comment = function () {
      $scope.myComment.saving = true;
      CommentResource.save({ resource: 'event', resourceId: $scope.resourceId, type: 'comment' },
        { text: $scope.myComment.text, reason: $scope.myComment.reason },
        function () {
          $scope.myComment.saving = false;
          $scope.myComment.text = '';

          $scope.comments = CommentResource.query({
            resource: 'event',
            resourceId: $scope.resourceId,
            type: 'comments'
          });
        }, function () {
          $scope.myComment.saving = false;
        }
      );
    };

    $scope.reply = function () {
      $scope.myComment.saving = true;
      CommentResource.save(
        {
          resource: 'event',
          resourceId: $scope.resourceId,
          id: $scope.replyToId,
          type: 'comment',
          reply: 'reply' },
        { text: $scope.myComment.text, resolved: $scope.myComment.resolved },
        function () {
          $scope.myComment.saving = false;
          $scope.myComment.text = '';

          $scope.comments = CommentResource.query({
            resource: 'event',
            resourceId: $scope.resourceId,
            type: 'comments'
          });
        }, function () {
          $scope.myComment.saving = false;
        }

      );
      $scope.exitReplyMode();
    };

    this.accessSaved = function () {
      Notifications.add('info', 'SAVED_ACL_RULES', NOTIFICATION_CONTEXT);
    };

    this.accessNotSaved = function () {
      Notifications.add('error', 'ACL_NOT_SAVED', NOTIFICATION_CONTEXT);

      $scope.access = EventAccessResource.get({ id: $scope.resourceId }, function (data) {
        if (angular.isDefined(data.episode_access)) {
          $scope.baseAclId = data.episode_access.current_acl.toString();
          var json = angular.fromJson(data.episode_access.acl);
          $scope.aclCreateDefaults.$promise.then(function () { // needed for roleUserPrefix
            changePolicies(json.acl.ace, true);
          });
        }
      });
    };

    $scope.accessSave = function () {
      var ace = [],
          hasRights = false,
          rulesValid = false;

      angular.forEach($scope.getAllPolicies(), function (policy) {
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
        EventAccessResource.save({id: $scope.resourceId}, {
          acl: {
            ace: ace
          },
          override: true
        }, me.accessSaved, me.accessNotSaved);
      }
      getCurrentPolicies();
    };

    let oldPolicies = {};
    let oldPoliciesUser = {};

    function getCurrentPolicies () {
      oldPolicies = JSON.parse(JSON.stringify($scope.policies));
      oldPoliciesUser = JSON.parse(JSON.stringify($scope.policiesUser));
    }

    function unsavedAccessChanges () {
      if (!policiesEqual(oldPolicies, $scope.policies)) {
        return true;
      }
      if (!policiesEqual(oldPoliciesUser, $scope.policiesUser)) {
        return true;
      }
      return false;
    }

    function policiesEqual(policies1, policies2) {
      if (policies1.length !== policies2.length) {
        return false;
      }

      let equal = true;
      policies1.forEach((policy1, index) => {
        const policy2 = policies2[index];

        if (policy1.role !== policy2.role) {
          equal = false;
        }
        else if (policy1.read !== policy2.read) {
          equal = false;
        }
        else if (policy1.write !== policy2.write) {
          equal = false;
        }

        if (policy1.actions.value.length !== policy2.actions.value.length) {
          equal = false;
          return;
        }
        policy1.actions.value.forEach((action1, index) => {
          const action2 = policy2.actions.value[index];
          if (action1 !== action2) {
            equal = false;
          }
        });
      });
      return equal;
    }

    $scope.statisticsCsvFileName = function (statsTitle) {
      var sanitizedStatsTitle = statsTitle.replace(/[^0-9a-z]/gi, '_').toLowerCase();
      return 'export_event_' + $scope.resourceId + '_' + sanitizedStatsTitle + '.csv';
    };

    $scope.severityColor = function (severity) {
      switch (severity.toUpperCase()) {
      case 'FAILURE':
        return 'red';
      case 'INFO':
        return 'green';
      case 'WARNING':
        return 'yellow';
      }
    };

    $scope.deleteComment = function (id) {
      CommentResource.delete(
        { resource: 'event', resourceId: $scope.resourceId, id: id, type: 'comment' },
        function () {
          $scope.comments = CommentResource.query({
            resource: 'event',
            resourceId: $scope.resourceId,
            type: 'comments'
          });
        }
      );
    };

    $scope.deleteCommentReply = function (commentId, reply) {
      CommentResource.delete(
        { resource: 'event', resourceId: $scope.resourceId, type: 'comment', id: commentId, reply: reply },
        function () {
          $scope.comments = CommentResource.query({
            resource: 'event',
            resourceId: $scope.resourceId,
            type: 'comments'
          });
        }
      );
    };

    checkForActiveTransactions();

    $scope.workflowAction = function (wfId, action) {
      if ($scope.workflowActionInProgress) return;
      $scope.workflowActionInProgress = true;
      EventWorkflowActionResource.save({id: $scope.resourceId, wfId: wfId, action: action}, function () {
        Notifications.add('success', 'EVENTS_PROCESSING_ACTION_' + action);
        $scope.close();
        $scope.workflowActionInProgress = false;
      }, function () {
        Notifications.add('error', 'EVENTS_PROCESSING_ACTION_NOT_' + action, NOTIFICATION_CONTEXT);
        $scope.workflowActionInProgress = false;
      });
    };

    $scope.deleteWorkflow = function (workflowId) {
      if ($scope.deleteWorkflowInProgress) return;
      $scope.deleteWorkflowInProgress = true;
      EventWorkflowDetailsResource.delete({ id0: $scope.resourceId, id1: workflowId },
        function () {
          Notifications.add('success', 'EVENTS_PROCESSING_DELETE_WORKFLOW', NOTIFICATION_CONTEXT);

          // We update our client-side model in case of success, so we don't have to send a new request
          if ($scope.workflows.entries) {
            $scope.workflows.entries = $scope.workflows.entries.filter(function (wf) {
              return wf.id !== workflowId;
            });
          }

          $scope.deleteWorkflowInProgress = false;
        }, function () {
          Notifications.add('error', 'EVENTS_PROCESSING_DELETE_WORKFLOW_FAILED', NOTIFICATION_CONTEXT);
          $scope.deleteWorkflowInProgress = false;
        });
    };

    $scope.isCurrentWorkflow = function (workflowId) {
      var currentWorkflow = $scope.workflows.entries[$scope.workflows.entries.length - 1];
      return currentWorkflow.id === workflowId;
    };

    $scope.$on('$destroy', function () {
      cleanupScopeResources();
    });
  }

]);
