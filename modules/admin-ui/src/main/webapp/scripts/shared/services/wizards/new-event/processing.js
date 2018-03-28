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
.factory('NewEventProcessing', ['$sce', 'NewEventProcessingResource', function ($sce, NewEventProcessingResource) {
    var Processing = function (use) {
        // Update the content of the configuration panel with the given HTML
        var me = this, queryParams,
            updateConfigurationPanel = function (html) {
                if (angular.isUndefined(html)) {
                    html = '';
                }
                me.workflowConfiguration = $sce.trustAsHtml(html);
            },
            isWorkflowSet = function () {
                return angular.isDefined(me.ud.workflow) && angular.isDefined(me.ud.workflow.id);
            },
            idConfigElement = '#new-event-workflow-configuration',
            workflowConfigEl = angular.element(idConfigElement);

        this.isProcessingState = true;
        this.ud = {};
        this.ud.workflow = {};

        // Object used for the workflow configurations
        window.ocWorkflowPanel = {};

        // Load all the workflows definitions
        if (use === 'tasks') {
            queryParams = {
                    tags: 'archive'
                };
        } else if (use === 'delete-event') {
            queryParams = {
                    tags: 'delete'
            };
        } else {
            queryParams = {
                tags: 'upload,schedule'
            };

        }
        NewEventProcessingResource.get(queryParams, function (data) {

            me.changingWorkflow = true;

            me.workflows = data.workflows;
            var default_workflow_id = data.default_workflow_id;

            // set default workflow as selected
            if(angular.isDefined(default_workflow_id)){

                for(var i = 0; i < me.workflows.length; i += 1){
                    var workflow = me.workflows[i];

                    if (workflow.id === default_workflow_id){
                      me.ud.workflow = workflow;
                      updateConfigurationPanel(me.ud.workflow.configuration_panel);
                      me.save();
                      break;
                    }
                }
            }
          me.changingWorkflow = false;

        });

        // Listener for the workflow selection
        this.changeWorkflow = function () {
            me.changingWorkflow = true;
            workflowConfigEl = angular.element(idConfigElement);
            if (angular.isDefined(me.ud.workflow)) {
                updateConfigurationPanel(me.ud.workflow.configuration_panel);
            } else {
                updateConfigurationPanel();
            }
            me.save();
            me.changingWorkflow = false;
        };

        // Get the workflow configuration
        this.getWorkflowConfig = function () {
            var workflowConfig = {}, element, isRendered = workflowConfigEl.find('.configField').length > 0;

            if (!isRendered) {
                element = angular.element(me.ud.workflow.configuration_panel).find('.configField');
            } else {
                element = workflowConfigEl.find('.configField');
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
        };

        this.isValid = function () {
            if (isWorkflowSet()) {
                return true;
            } else {
                return false;
            }
        };

        // Save the workflow configuration
        this.save = function () {

            if (isWorkflowSet()) {
                me.ud.workflow.selection  = {
                    id: me.ud.workflow.id,
                    configuration: me.getWorkflowConfig()
                };
            }
        };

        this.reset = function () {
            me.isProcessingState = true;
            me.ud = {};
            me.ud.workflow = {};
            me.workflows = {};
        };

        this.getUserEntries = function () {
            return me.ud.workflow;
        };
    };

    return {
        get: function (use) {
            return new Processing(use);
        }
    };

}]);
