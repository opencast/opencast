describe('Bulk Message Controller', function () {
    var $scope, $controller, $parentScope, $httpBackend, Table, Modal,
        Notifications, EVENT_TAB_CHANGE;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate:     function (val, time) { return time; },
            formatDateTime: function (val, time) { return time; },
            formatTime:     function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _Table_, _Modal_, _Notifications_, _EVENT_TAB_CHANGE_) {
        $controller = _$controller_;
        Notifications = _Notifications_;
        Table = _Table_;
        Modal = _Modal_;
        $httpBackend = _$httpBackend_;
        EVENT_TAB_CHANGE = _EVENT_TAB_CHANGE_;

        $parentScope = $rootScope.$new();
        $scope = $parentScope.$new();
        $controller('BulkMessageCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/email/templates.json')
            .respond('[]');
        $httpBackend.whenGET('/email/variables.json')
            .respond('[]');
        $httpBackend.whenGET('/admin-ng/event/events/sendmessage?eventIds=')
            .respond('{}');
        $httpBackend.whenGET('/admin-ng/event/events/recipients?eventIds=')
            .respond('{}');
    });

    it('instantiates and provides the states', function () {
        expect($scope.states).toBeDefined();
    });

    describe('on EVENT_TAB_CHANGE', function () {
        beforeEach(function () {
            $scope.states = [{}, {
                stateController: { updatePreview: jasmine.createSpy() }
            }];
        });

        it('reacts when changing to the summary state', function () {
            $scope.$emit(EVENT_TAB_CHANGE, { current: { name: 'summary' } });
            expect($scope.states[1].stateController.updatePreview).toHaveBeenCalled();
        });

        it('does not react otherwise', function () {
            $scope.$emit(EVENT_TAB_CHANGE, { current: { name: 'foo' } });
            expect($scope.states[1].stateController.updatePreview).not.toHaveBeenCalled();
        });
    });

    describe('#submit', function () {
        beforeEach(function () {
            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');
            Modal.$scope = { close: jasmine.createSpy() };

            $scope.states = [{
                name: 'recipients',
                stateController: { ud: {
                    items: {
                        recipients: [ { id: 912 } ],
                        recordings: [ { id: 21 } ]
                    }
                } }
            }, {
                name: 'message',
                stateController: { ud: {
                    email_template: { id: 751 }
                } }
            }, {
                name: 'summary',
                stateController: { ud: {
                    email_template: { id: 751 }
                } }
            }];
        });

        it('saves collected userdata', function () {
            $httpBackend.expectPOST('/email/send/751').respond(201);
            $scope.submit();
            $httpBackend.flush();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenPOST('/email/send/751').respond(201);
            });

            it('shows a notification and refreshes the table', function () {
                $scope.submit();
                $httpBackend.flush();

                expect(Table.fetch).toHaveBeenCalled();
                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
            });

            it('closes the modal', function () {
                $scope.submit();
                $httpBackend.flush();

                expect(Modal.$scope.close).toHaveBeenCalled();
            });
        });

        describe('on error', function () {
            beforeEach(function () {
                $httpBackend.whenPOST('/email/send/751').respond(500);
            });

            it('shows a notification', function () {
                $scope.submit();
                $httpBackend.flush();

                expect(Table.fetch).not.toHaveBeenCalled();
                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String), jasmine.any(String));
            });
        });
    });
});
