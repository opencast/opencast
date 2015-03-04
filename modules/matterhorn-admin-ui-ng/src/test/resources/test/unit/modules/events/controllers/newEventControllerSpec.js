describe('New Event Controller', function () {
    var $scope, $parentScope, $httpBackend, Modal, Table, Notifications, EVENT_TAB_CHANGE;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate:     function (val, date) { return date; },
            formatDateTime: function (val, date) { return date; },
            formatTime:     function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _Modal_, _Table_, _Notifications_, _EVENT_TAB_CHANGE_) {
        $httpBackend = _$httpBackend_;
        Modal = _Modal_;
        Table = _Table_;
        Notifications = _Notifications_;
        EVENT_TAB_CHANGE = _EVENT_TAB_CHANGE_;

        $parentScope = $rootScope.$new();
        $scope = $parentScope.$new();
        $controller('NewEventCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    it('sports a submit function', function () {
        expect($scope.submit).toBeDefined();
    });

    describe('on EVENT_TAB_CHANGE', function () {
        function prepareScope(isProcessingState) {
            return {
                name: 'myState',
                stateController: {
                    save: jasmine.createSpy(),
                    isProcessingState: isProcessingState
                }
            };
        }

        it('reacts when the previous state is processing', function () {
            var oldState = prepareScope(true);
            $scope.$emit(EVENT_TAB_CHANGE, {
                old: oldState,
                current: {
                    stateController: {}
                }
            });
            expect(oldState.stateController.save).toHaveBeenCalled();
        });

        it('does not react otherwise', function () {
            var oldState = prepareScope(false);
            $scope.$emit(EVENT_TAB_CHANGE, {
                old: oldState,
                current: {
                    stateController: {}
                }
            });
            expect(oldState.stateController.save).not.toHaveBeenCalled();
        });
    });

    describe('#submit', function () {
        beforeEach(function () {
            $httpBackend.whenGET('/admin-ng/event/new/metadata').respond('{}');
            $httpBackend.whenGET('/admin-ng/capture-agents/agents.json?inputs=true')
                .respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
            $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond('{}');
            $httpBackend.whenGET('/admin-ng/resources/ROLES.json').respond('{}');
            $httpBackend.whenGET('/workflow/definitions.json').respond('{}');

            Modal.$scope = { close: jasmine.createSpy() };

            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');

            $scope.states = [{
                name: 'source',
                stateController: {
                    ud: {
                        type: 'foo',
                        foo: {
                            start: '2014-07-07',
                            device: {
                                id: 'a device id'
                            }
                        }
                    }
                }
            }, {
                name: 'processing',
                stateController: { ud: { workflow: { selection: 'bar' } } }
            }, {
                name: 'metadata',
                stateController: { ud: 'bar' }
            }, {
                name: 'access',
                stateController: { ud: { access: { acl: 345 }} }
            }];
        });

        it('saves collected userdata', function () {
            $httpBackend.whenGET('/admin-ng/event/new/access').respond('{access: { access: {acl: 345}}}');
            $httpBackend.expectGET('/admin-ng/event/new/processing?tags=upload-ng,schedule-ng').respond(200);
            $httpBackend.expectPOST('/admin-ng/event/new').respond(201);
            $scope.submit();
            $httpBackend.flush();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenGET('/admin-ng/event/new/access').respond('{access: { access: {acl: 345}}}');
                $httpBackend.expectGET('/admin-ng/event/new/processing?tags=upload-ng,schedule-ng').respond(200);
                $httpBackend.whenPOST('/admin-ng/event/new').respond(201);
            });

            it('shows a notification', function () {
                $scope.submit();
                expect(Notifications.add).toHaveBeenCalledWith('success', 'EVENTS_UPLOAD_STARTED', 'global', -1);
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('success', 'EVENTS_CREATED');
            });

            it('closes the modal', function () {
                $scope.submit();
                $httpBackend.flush();

                expect(Modal.$scope.close).toHaveBeenCalled();
            });
        });

        describe('on error', function () {
            beforeEach(function () {
                $httpBackend.whenGET('/admin-ng/event/new/access').respond('{access: { access: {acl: 345}}}');
                $httpBackend.expectGET('/admin-ng/event/new/processing?tags=upload-ng,schedule-ng').respond(200);
                $httpBackend.whenPOST('/admin-ng/event/new').respond(500);
            });

            it('shows a notification', function () {
                $scope.submit();
                expect(Notifications.add).toHaveBeenCalledWith('success', 'EVENTS_UPLOAD_STARTED', 'global', -1);
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('error', 'EVENTS_NOT_CREATED');
            });
        });
    });
});
