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
  'EventWorkflowDetailsResource', 'ResourcesListResource', 'UserRolesResource', 'EventAccessResource',
  'EventPublicationsResource', 'EventSchedulingResource','NewEventProcessingResource', 'CaptureAgentsResource',
  'ConflictCheckResource', 'Language', 'JsHelper', '$sce', '$timeout', 'EventHelperService', 'UploadAssetOptions',
  'EventUploadAssetResource', 'Table', 'SchedulingHelperService', 'StatisticsReusable',
  function ($scope, Notifications, EventTransactionResource, EventMetadataResource, EventAssetsResource,
    EventAssetCatalogsResource, CommentResource, EventWorkflowsResource, EventWorkflowActionResource,
    EventWorkflowDetailsResource, ResourcesListResource, UserRolesResource, EventAccessResource,
    EventPublicationsResource, EventSchedulingResource, NewEventProcessingResource, CaptureAgentsResource,
    ConflictCheckResource, Language, JsHelper, $sce, $timeout, EventHelperService, UploadAssetOptions,
    EventUploadAssetResource, Table, SchedulingHelperService, StatisticsReusable) {

    var roleSlice = 100;
    var roleOffset = 0;
    var loading = false;
    var rolePromise = null;

    var saveFns = {},
        me = this,
        NOTIFICATION_CONTEXT = 'events-access',
        SCHEDULING_CONTEXT = 'event-scheduling',
        mainCatalog = 'dublincore/episode',
        idConfigElement = '#event-workflow-configuration',
        workflowConfigEl = angular.element(idConfigElement),
        baseWorkflow,
        createPolicy = function (role) {
          return {
            role  : role,
            read  : false,
            write : false,
            actions : {
              name : 'event-acl-actions',
              value : []
            }
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
            return role.startsWith('ROLE_USER_') && role != 'ROLE_USER_ADMIN';
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
            $scope.policies.push(policy);
          });

          if (!loading) {
            $scope.accessSave();
          }
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
        updateRoles = function() {
          //MH-11716: We have to wait for both the access (series ACL), and the roles (list of system roles)
          //to resolve before we can add the roles that are present in the series but not in the system
          return ResourcesListResource.get({ resource: 'ROLES' }, function (results) {
            var roles = results;
            return $scope.access.$promise.then(function () {
              angular.forEach($scope.access.episode_access.privileges, function(value, key) {
                if (angular.isUndefined(roles[key])) {
                  roles[key] = key;
                }
              }, this);
              return roles;
            }).catch(angular.noop);
          }, this);
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
            var episodeCatalogIndex;
            angular.forEach(metadata.entries, function (catalog, index) {
              if (catalog.flavor === mainCatalog) {
                $scope.episodeCatalog = catalog;
                episodeCatalogIndex = index;
                var keepGoing = true;
                var tabindex = 2;
                angular.forEach(catalog.fields, function (entry) {
                  if (entry.id === 'title' && angular.isString(entry.value)) {
                    $scope.titleParams = { resourceId: entry.value.substring(0,70) };
                  }
                  if (keepGoing && entry.locked) {
                    metadata.locked = entry.locked;
                    keepGoing = false;
                  }
                  entry.tabindex = tabindex ++;
                });
              }
            });

            if (angular.isDefined(episodeCatalogIndex)) {
              metadata.entries.splice(episodeCatalogIndex, 1);
            }
          });

          //<===============================
          // Enable asset upload (catalogs and attachments) to existing events

          // Retrieve option configuration for asset upload
          UploadAssetOptions.getOptionsPromise().then(function(data){
            if (data) {
              $scope.assetUploadWorkflowDefId = data.workflow;
              $scope.uploadAssetOptions = [];
              // Filter out asset options of type "track".
              // Not allowing tracks to be added to existing mediapackages
              // for this iteration of the upload option feature.
              // TODO: consider enabling track uploads to existing mps.
              angular.forEach(data.options, function(option) {
                if (option.type !== 'track') {
                  $scope.uploadAssetOptions.push(option);
                }
              });
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
          $scope.roles = updateRoles();

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
              var json = angular.fromJson(data.episode_access.acl);
              changePolicies(json.acl.ace, true);
            }
          });
          $scope.comments = CommentResource.query({ resource: 'event', resourceId: id, type: 'comments' });
        },
        tzOffset = (new Date()).getTimezoneOffset() / -60;

    $scope.statReusable = null;

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
      }).catch(angular.noop
      ).finally(function () {
        loading = false;
      });
      return rolePromise;
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

    $scope.saveScheduling = function () {
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
            entries: $scope.source
          }, function () {
            fetchChildResources($scope.resourceId);
          });
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
    $scope.baseAcl = {};

    $scope.changeBaseAcl = function () {
      $scope.baseAcl = EventAccessResource.getManagedAcl({id: this.baseAclId}, function () {
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
    $scope.getSaveFunction = function (flavor) {
      var fn = saveFns[flavor],
          catalog;

      if (angular.isUndefined(fn)) {
        if ($scope.episodeCatalog.flavor === flavor) {
          catalog = $scope.episodeCatalog;
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

    $scope.metadataSave = function (id, callback, catalog) {
      catalog.attributeToSend = id;
      EventMetadataResource.save({ id: $scope.resourceId }, catalog,  function () {
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
          var json = angular.fromJson(data.episode_access.acl);
          changePolicies(json.acl.ace, true);
        }
      });
    };

    $scope.accessChanged = function (role) {
      if (!role) return;
      $scope.accessSave();
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
        EventAccessResource.save({id: $scope.resourceId}, {
          acl: {
            ace: ace
          },
          override: true
        }, me.accessSaved, me.accessNotSaved);
      }
    };

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
