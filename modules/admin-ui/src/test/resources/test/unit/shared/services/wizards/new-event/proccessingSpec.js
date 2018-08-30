describe('Processing Step in New Event Wizard', function () {
    var NewEventProcessing, $sce;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewEventProcessing_, _$sce_) {
        NewEventProcessing = _NewEventProcessing_.get();
        $sce = _$sce_;
    }));

    describe('#isValid', function () {

        it('is invalid by default', function () {
            expect(NewEventProcessing.isValid()).toBeFalsy();
        });

        it('becomes valid when a workflow id is set', function () {
            NewEventProcessing.ud.workflow.id = 'workflow';
            expect(NewEventProcessing.isValid()).toBe(true);
        });
    });

    describe('#changeWorkflow', function () {

        describe('with a workflow', function () {
            var configPanel = 'panel';
            beforeEach(function () {
                NewEventProcessing.ud.workflow = {
                    id: 'default',
                    configuration_panel: configPanel
                };
            });

            it('set workflow configuration panel when a workflow is chosen.', function () {
                NewEventProcessing.changeWorkflow();
                expect(NewEventProcessing.workflowConfiguration.$$unwrapTrustedValue())
                    .toEqual($sce.trustAsHtml(configPanel).$$unwrapTrustedValue());
            });
        });

        describe('without a workflow', function () {
            beforeEach(function () {
                delete NewEventProcessing.ud.workflow;
            });

            it('sets an empty workflow configuration panel', function () {
                expect(NewEventProcessing.workflowConfiguration).toBeUndefined();
                NewEventProcessing.changeWorkflow();
                expect(NewEventProcessing.workflowConfiguration).toEqual('');
            });
        });
    });

    describe('#save', function () {

        it('saves workflow in userdata when workflow is set', function () {
            var workflowId = 'default';
            NewEventProcessing.ud.workflow.id = workflowId;
            NewEventProcessing.save();
            expect(NewEventProcessing.ud.workflow.id).toBe(workflowId);
        });
    });

    describe('#getWorkflowConfig', function () {
        beforeEach(function () {
            $('body').append('<div id="new-event-workflow-configuration"></div>');
            NewEventProcessing.changeWorkflow();
        });

        afterEach(function () {
            $('#new-event-workflow-configuration').remove();
        });

        describe('with a checked checkbox', function () {
            beforeEach(function () {
                $('#new-event-workflow-configuration')
                    .append('<input type="checkbox" checked="checked" class="configField" id="testID" value="testvalueA">');
            });

            it('returns the field value', function () {
                expect(NewEventProcessing.getWorkflowConfig())
                    .toEqual({ testID: 'true' });
            });
        });

        describe('with an unchecked checkbox', function () {
            beforeEach(function () {
                $('#new-event-workflow-configuration')
                    .append('<input type="checkbox" class="configField" id="testID" value="testvalueA">');
            });

            it('returns the field value', function () {
                expect(NewEventProcessing.getWorkflowConfig()).toEqual({ testID: 'false' });
            });
        });

        describe('without a checkbox', function () {
            beforeEach(function () {
                $('#new-event-workflow-configuration')
                    .append('<input type="text" class="configField" id="testID" value="testvalueA">');
            });

            it('returns the field value', function () {
                expect(NewEventProcessing.getWorkflowConfig())
                    .toEqual({ testID: 'testvalueA' });
            });
        });
    });

    describe('#getUserEntries', function () {
        beforeEach(function () {
            NewEventProcessing.ud.workflow.foo = 'bar';
        });

        it('returns the user data', function () {
            expect(NewEventProcessing.getUserEntries()).toEqual({ foo: 'bar' });
        });
    });
});
