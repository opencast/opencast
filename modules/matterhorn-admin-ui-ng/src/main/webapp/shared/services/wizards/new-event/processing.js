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

        // Load all the worklfows definitions
        if (use === 'tasks') {
            queryParams = {
                    tags: 'archive-ng'
                };
        } else {
            queryParams = {
                tags: 'upload-ng,schedule-ng'
            };
        }
        NewEventProcessingResource.get(queryParams, function (data) {
            me.workflows = data;
        });

        // Listener for the workflow selection
        this.changeWorkflow = function () {
            workflowConfigEl = angular.element(idConfigElement);
            if (angular.isDefined(me.ud.workflow)) {
                updateConfigurationPanel(me.ud.workflow.configuration_panel);
            } else {
                updateConfigurationPanel();
            }
            me.save();
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
