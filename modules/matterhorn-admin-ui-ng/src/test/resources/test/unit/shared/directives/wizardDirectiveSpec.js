describe('adminNg.directives.wizardDirective', function () {
    var $compile, $rootScope, $parentScope, $scope, $timeout, $resource, $httpBackend, element, TestController;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/wizard.html'));
    beforeEach(module('shared/partials/notification.html'));
    beforeEach(module('shared/partials/notifications.html'));
    beforeEach(module('shared/partials/wizards/new-event.html'));
    beforeEach(module('shared/partials/editable.html'));
    beforeEach(module('shared/partials/editableBooleanValue.html'));
    beforeEach(module('shared/partials/editableDateValue.html'));
    beforeEach(module('shared/partials/editableSingleValue.html'));
    beforeEach(module('shared/partials/editableSingleSelect.html'));
    beforeEach(module('shared/partials/editableMultiValue.html'));
    beforeEach(module('shared/partials/editableMultiSelect.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$rootScope_, _$timeout_, _$compile_, _$resource_, _$httpBackend_) {
        $compile = _$compile_;
        $timeout = _$timeout_;
        $rootScope = _$rootScope_;
        $httpBackend = _$httpBackend_;
        $parentScope = $rootScope.$new();

        $scope = $parentScope.$new();
        $scope.$parent = $parentScope;
        $parentScope.openTab = function () {};
        $resource = _$resource_;
        TestController = function (isValid) {
            var valid  = isValid;
            this.visible = true;
            this.isValid = function () {
                return valid;
            };
            this.save = function () {
                return true;
            };
        };
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/components.json')));
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/ACL.json')));
        $httpBackend.whenGET('/admin-ng/event/new/metadata').respond(JSON.stringify(getJSONFixture('admin-ng/event/new/metadata')));
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json?inputs=true').respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
        $httpBackend.whenGET('/workflow/definitions.json').respond(JSON.stringify(getJSONFixture('workflow/definitions.json')));
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
    });

    describe('if all steps are valid', function () {
        beforeEach(function () {
            $scope.states = [
                {
                    translation: 'EVENTS.NEW.METADATA.CAPTION',
                    name: 'metadata',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.METADATA.CAPTION',
                    name: 'metadata-extended',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.SOURCE.CAPTION',
                    name: 'source',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.PROCESSING.CAPTION',
                    name: 'processing',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.ACCESS.CAPTION',
                    name: 'access',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.SUMMARY.CAPTION',
                    name: 'summary',
                    stateController: (function () {
                        var tc = new TestController(true);
                        tc.test = 'check';
                        return tc;
                    })()
                }
            ];

            element = $compile('<admin-ng-wizard data-name="new-event" data-states="states"></admin-ng-wizard>')($scope);
            $parentScope.$digest();
            $scope.$digest();
            $rootScope.$digest();
        });

        it('instantiates a wizard object', function () {
            element.find('a[data-modal-tab="source"]').click();
            expect(element.isolateScope().wizard).toBeDefined();
        });

        it('creates a navigation menu', function () {
            expect(element.find('nav#modal-nav')).toExist();
            expect(element.find('nav#breadcrumb')).toExist();
        });

        it('switches tabs when clicked', function () {
            spyOn($parentScope, 'openTab');
            element.find('a[data-modal-tab="source"]').click();
            expect($parentScope.openTab).toHaveBeenCalled();
        });

        it('starts with the first state metadata', function () {
            expect(element.isolateScope().wizard.getCurrentState().name).toEqual('metadata');
        });

        it('changes state when a menu item is clicked', function () {
            element.find('a[data-modal-tab="summary"]').click();
            expect(element.isolateScope().wizard.getCurrentState().stateController.test).toEqual('check');
        });

        it('navigates to the next step when the next button is clicked', function () {
            element.find('a[data-modal-tab="source"]').click();
            element.find('a[data-modal-tab="next"]').click();
            expect(element.isolateScope().wizard.getCurrentState().name).toEqual('processing');
        });

        it('navigates to the last step when the back button is clicked', function () {
            element.find('a[data-modal-tab="source"]').click();
            element.find('a[data-modal-tab="previous"]').click();
            expect(element.isolateScope().wizard.getCurrentState().name).toEqual('metadata-extended');
        });

        it('propagates the save action', function () {
            var saveMethod = element.isolateScope().wizard.getCurrentState().stateController;
            spyOn(saveMethod, 'save');
            element.isolateScope().wizard.save(null);
            expect(saveMethod.save).toHaveBeenCalled();
        });


    });

    describe('the first step is invalid', function () {
        beforeEach(function () {
            $scope.states = [
                {
                    translation: 'EVENTS.NEW.METADATA.CAPTION',
                    name: 'metadata',
                    stateController: new TestController(false)
                },
                {
                    translation: 'EVENTS.NEW.METADATA.CAPTION',
                    name: 'metadata-extended',
                    stateController: new TestController(false)
                },
                {
                    translation: 'EVENTS.NEW.SOURCE.CAPTION',
                    name: 'source',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.PROCESSING.CAPTION',
                    name: 'processing',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.ACCESS.CAPTION',
                    name: 'access',
                    stateController: new TestController(true)
                },
                {
                    translation: 'EVENTS.NEW.SUMMARY.CAPTION',
                    name: 'summary',
                    stateController: new TestController(true)
                }
            ];
            element = $compile('<admin-ng-wizard data-name="new-event" data-states="states"></admin-ng-wizard>')($scope);
            $parentScope.$digest();
            $scope.$digest();
            $rootScope.$digest();
        });

        it('does not navigate to a step unless all previous states are valid', function () {
            spyOn($parentScope, 'openTab');
            element.find('button[data-modal-tab="next"]').click();
            expect($parentScope.openTab).not.toHaveBeenCalled();
        });

        it('gracefully handles misconfigurations', function () {
            var $event = {
                target: {
                    getAttribute: function () {
                        return 'nope';
                    }
                }
            };
            element.isolateScope().wizard.toTab($event);
            expect(element.isolateScope().wizard.getCurrentState().name).toEqual('metadata');
        });

        it('does not allow to leave out an invalid step', function () {
            var $event = {
                target: {
                    getAttribute: function () {
                        return 'access';
                    }
                }
            };
            element.isolateScope().wizard.toTab($event);
            expect(element.isolateScope().wizard.getCurrentState().name).toEqual('metadata');
        });
    });

});
