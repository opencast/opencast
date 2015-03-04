describe('Email Template Controller', function () {
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
        $controller('EmailtemplateCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/EMAIL_TEMPLATE_NAMES.json')
            .respond('{}');
        $httpBackend.whenGET('/email/variables.json')
            .respond('[]');
    });

    it('instantiates and provides the states', function () {
        expect($scope.states).toBeDefined();
    });

    describe('when editing', function () {
        beforeEach(function () {
            $scope.action = 'edit';
            Modal.$scope = { resourceId: '380' };
        });

        it('fetches email template data', function () {
            $httpBackend.expectGET('/email/template/380')
                .respond(JSON.stringify(getJSONFixture('email/template/1101')));
            $controller('EmailtemplateCtrl', {$scope: $scope});
            $httpBackend.flush();
        });

        it('populates user data', function () {
            $httpBackend.whenGET('/email/template/380')
                .respond(JSON.stringify(getJSONFixture('email/template/1101')));
            $controller('EmailtemplateCtrl', {$scope: $scope});
            $httpBackend.flush();

            expect($scope.states[0].stateController.ud.name).toEqual('•mock• New Template');
        });
    });

    describe('on EVENT_TAB_CHANGE', function () {
        beforeEach(function () {
            $scope.states = [{
                stateController: { updatePreview: jasmine.createSpy() }
            }];
        });

        it('reacts when changing to the summary state', function () {
            $scope.$emit(EVENT_TAB_CHANGE, { current: { name: 'summary' } });
            expect($scope.states[0].stateController.updatePreview).toHaveBeenCalled();
        });

        it('does not react otherwise', function () {
            $scope.$emit(EVENT_TAB_CHANGE, { current: { name: 'foo' } });
            expect($scope.states[0].stateController.updatePreview).not.toHaveBeenCalled();
        });
    });

    describe('#submit', function () {
        beforeEach(function () {
            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');
            Modal.$scope = { close: jasmine.createSpy() };

            $scope.states = [{
                name: 'items',
                stateController: { ud: { items: [ { id: 912 } ] } }
            }, {
                name: 'dates',
                stateController: { ud: {
                    fromDate: '2014-07-01',
                    toDate: '2014-12-01',
                    fromTime: '10:54',
                    toTime: '23:31'
                } }
            }, {
                name: 'reason',
                stateController: { ud: { fancy: 'permission' } }
            }];
        });

        describe('when creating', function () {

            it('saves collected userdata', function () {
                $httpBackend.expectPOST('/email/template').respond(201);
                $scope.submit();
                $httpBackend.flush();
            });

            describe('on success', function () {
                beforeEach(function () {
                    $httpBackend.whenPOST('/email/template').respond(201);
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
                    $httpBackend.whenPOST('/email/template').respond(500);
                });

                it('shows a notification', function () {
                    $scope.submit();
                    $httpBackend.flush();

                    expect(Table.fetch).not.toHaveBeenCalled();
                    expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String), jasmine.any(String));
                });
            });
        });

        describe('when editing', function () {
            beforeEach(function () {
                $scope.action = 'edit';
            });

            it('saves collected userdata', function () {
                $httpBackend.expectPUT('/email/template').respond(201);
                $scope.submit();
                $httpBackend.flush();
            });

            describe('on success', function () {
                beforeEach(function () {
                    $httpBackend.whenPUT('/email/template').respond(201);
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
                    $httpBackend.whenPUT('/email/template').respond(500);
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
});
