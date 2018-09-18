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
.factory('NewEventProcessing', ['$sce', '$timeout', 'NewEventProcessingResource', function ($sce, $timeout, NewEventProcessingResource) {
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
            originalValues = {};

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
            me.default_workflow_id = data.default_workflow_id;

            me.changingWorkflow = false;

        });

        // Execute function for each input HTML element that has an ID
        // (so is used in the workflow configuration form).
        function forEachHtmlFormElement(htmlElement, f) {
            htmlElement.each(function (idx, domElement) {
                var angularElement = angular.element(domElement);
                var idAttr = angularElement.attr('id');

                // Ignore input fields that don't have an ID
                if (angular.isDefined(idAttr)) {
                    f(idAttr, angularElement);
                }
            });
        }

        // Collect all radio elements (which are linked by their name
        // attribute) into a dictionary:
        // id: array of other radio ids.
        function gatherRadios(htmlElement) {
            var result = {};
            forEachHtmlFormElement(htmlElement, function(id, angularElement) {
                if (angularElement.is('[type=radio]')) {
                    var radioName = angularElement.attr('name');
                    var radios = angular.element(document).find('input[name='+radioName+']');
                    result[id] = [];
                    radios.each(function(ridx, radio) {
                        result[id].push(angular.element(radio).attr('id'));
                   });
                }
            });
            return result;
        }

        // Determine if all elements of an array are the same (returns
        // true for empty arrays).
        function allTheSame(a) {
            if (a.length === 0) {
                return true;
            }
            var first = a[0];
            for(var i = 0; i < a.length; i++) {
                if (a[i] !== first) {
                    return false;
                }
            }
            return true;
        }

        // Take a dictionary collected by gatherHtmlFormElements and
        // nullify all radio elements that are ambiguous (i.e. are all
        // unchecked).
        function determineIndeterminateRadios(htmlFields, radios) {
            for(var radioId in radios) {
                if (htmlFields[radioId] === null) {
                    continue;
                }
                var radioValues = [];
                for (var i = 0; i < radios[radioId].length; i++) {
                    radioValues.push(htmlFields[radios[radioId][i]]);
                }
                if (allTheSame(radioValues)) {
                    for (var j = 0; j < radios[radioId].length; j++) {
                        htmlFields[radios[radioId][j]] = null;
                    }
                }
            }
        }

        // Most of the stuff we do here with input elements doesn't distinguish between
        // input type="text" and input type="number", so we need this getter a few times.
        function elementIsTextual(angularElement) {
            return angularElement.is("[type=text]") || angularElement.is("[type=number]");
        }

        // Gather all input elements used in the workflow configuration HTML into a dictionary:
        // id: value or null if it's indeterminate
        function gatherHtmlFormElements(htmlElement) {
            var result = {};
            forEachHtmlFormElement(htmlElement, function(id, angularElement) {
                if (elementIsTextual(angularElement)) {
                    if (angularElement.val() === '') {
                        result[id] = null;
                    } else {
                        result[id] = angularElement.val();
                    }
                } else if (angularElement.is('[type=checkbox]') || angularElement.is('[type=radio]')) {
                    if (angularElement.prop('indeterminate')) {
                        result[id] = null;
                    } else {
                        if (angularElement.is(':checked')) {
                            result[id] = "true";
                        }
                        else {
                            result[id] = "false";
                        }
                    }
                }
            });
            return result;
        }

        // Take a dictionary of event properties and extract all
        // values of a single property into an array.
        function eventValuesForField(events, searchProp) {
            var result = [];
            for(var eid in events) {
                for (var evProp in events[eid]) {
                    if (evProp === searchProp) {
                        result.push(events[eid][evProp]);
                    }
                }
            }
            return result;
        }

        // Filter original workflow properties and make proper
        // dictionaries out of them so they can be iterated over easily.
        function filterEventProperties(workflowProperties, selectedIds) {
          var result = {};
          for (var workflowProp in workflowProperties) {
            if (workflowProp.indexOf("$") !== 0 && workflowProperties.hasOwnProperty(workflowProp)) {
              if (selectedIds.indexOf(workflowProp) >= 0) {
                  result[workflowProp] = workflowProperties[workflowProp];
              }
            }
          }
          return result;
        }

        // Set a HTML form value (abstracts over "set checked" or "set
        // value" for checkboxes and text fields, respectively)
        function setHtmlFormValue(angularElement, value) {
            if (elementIsTextual(angularElement)) {
                angularElement.val(value);
            } else if (angularElement.is("[type=checkbox]") || angularElement.is("[type=radio]")) {
                if (value === "true") {
                    angularElement.attr("checked", true);
                } else {
                    angularElement.attr("checked", false);
                }
            }
        }

        // Set an indeterminate HTML form value (depends on the type
        // of the form element)
        function setIndeterminateHtmlFormValue(angularElement) {
            if (elementIsTextual(angularElement)) {
                angularElement.val("");
            } else if (angularElement.is("[type=checkbox]")) {
                angularElement.prop("indeterminate", true);
            } else if (angularElement.is("[type=radio]")) {
                angularElement.attr("checked", false);
            }
        }

        this.applyWorkflowProperties = function(workflowProperties, selectedIds) {
            // Timeout because manipulating the just assigned HTML doesn't work otherwise.
            $timeout(function() {
                var element,
                    workflowConfigEl = angular.element(idConfigElement),
                    isRendered = workflowConfigEl.find('.configField').length > 0;
                if (!isRendered) {
                    element = angular.element(me.ud.workflow.configuration_panel).find('.configField');
                } else {
                    element = workflowConfigEl.find('.configField');
                }

                var htmlFields = gatherHtmlFormElements(element);
                me.currentEvents = filterEventProperties(workflowProperties, selectedIds);

		// Set event properties that are in the HTML form, but not in the event, yet.
                for(var eventId in me.currentEvents) {
                    var eventProps = me.currentEvents[eventId];
                    for(var htmlField in htmlFields) {
                        if (!(htmlField in eventProps)) {
                            eventProps[htmlField] = htmlFields[htmlField];
                        }
                    }
                }
                // Only manipulate HTML elements if events are present
                // (not the case for "Add event")
                if (selectedIds !== undefined && selectedIds.length > 0) {
                    forEachHtmlFormElement(element, function(id, angularElement) {
                        var valuesForId = eventValuesForField(me.currentEvents, id);

                        if(allTheSame(valuesForId)) {
                            setHtmlFormValue(angularElement, valuesForId[0]);
                        } else {
                            setIndeterminateHtmlFormValue(angularElement);
                        }
                    });
                }
            });
        };

        this.initWorkflowConfig = function (workflowProperties, selectedIds) {
            // set default workflow as selected
            if(angular.isDefined(me.default_workflow_id)){

                for(var i = 0; i < me.workflows.length; i += 1){
                    var workflow = me.workflows[i];

                    if (workflow.id === me.default_workflow_id){
                        me.ud.workflow = workflow;
                        updateConfigurationPanel(me.ud.workflow.configuration_panel);
                        this.applyWorkflowProperties(workflowProperties, selectedIds);
                        me.save();
                        break;
                    }
                }
            } else {
                me.ud.workflow = {};
                delete me.workflowConfiguration;
            }
        };

        // Listener for the workflow selection
        this.changeWorkflow = function (workflowProperties, selectedIds) {
            originalValues = {};
            me.changingWorkflow = true;
            if (angular.isDefined(me.ud.workflow)) {
                updateConfigurationPanel(me.ud.workflow.configuration_panel);
            } else {
                updateConfigurationPanel();
            }
            this.applyWorkflowProperties(workflowProperties, selectedIds);

            me.save();
            me.changingWorkflow = false;
        };

        // This is used for the new task post request
        this.getWorkflowConfigs = function () {
            var workflowConfigs = {},
                element,
                workflowConfigEl = angular.element(idConfigElement),
                isRendered = workflowConfigEl.find('.configField').length > 0;

            if (!isRendered) {
                element = angular.element(me.ud.workflow.configuration_panel).find('.configField');
            } else {
                element = workflowConfigEl.find('.configField');
            }

            var radios = gatherRadios(element);
            var htmlFields = gatherHtmlFormElements(element);
            determineIndeterminateRadios(htmlFields, radios);

            for(var eventId in me.currentEvents) {
                var eventProps = me.currentEvents[eventId];
                for(var htmlField in htmlFields) {
                    var htmlValue = htmlFields[htmlField];
                    if (htmlValue !== null) {
                        eventProps[htmlField] = htmlValue;
                    }
                }
                workflowConfigs[eventId] = eventProps;
            }

            return workflowConfigs;
        };

        // Get the workflow configuration (used for the final value table in the wizard)
        this.getWorkflowConfig = function () {
            var workflowConfig = {},
                element,
                workflowConfigEl = angular.element(idConfigElement),
                isRendered = workflowConfigEl.find('.configField').length > 0;

            if (!isRendered) {
                element = angular.element(me.ud.workflow.configuration_panel).find('.configField');
            } else {
                element = workflowConfigEl.find('.configField');
            }

            var radios = gatherRadios(element);
            var htmlFields = gatherHtmlFormElements(element);
            determineIndeterminateRadios(htmlFields, radios);
            for (var fieldId in htmlFields) {
                var htmlField = htmlFields[fieldId];
                if (htmlField === null) {
                    workflowConfig[fieldId] = "*";
                } else {
                    workflowConfig[fieldId] = htmlField;
                }
            }
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
