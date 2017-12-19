describe('Location Blacklist Controller', function () {
    var $scope, $controller, $parentScope, $httpBackend, Table, Modal, Notifications;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate:          function (val, date) { return date; },
            formatDateTime:      function (val, date) { return date; },
            formatDateTimeRaw:   function (val, date) { return date; },
            formatTime:          function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _Table_, _Modal_, _Notifications_) {
        $controller = _$controller_;
        Notifications = _Notifications_;
        Modal = _Modal_;
        Table = _Table_;
        $httpBackend = _$httpBackend_;

        $parentScope = $rootScope.$new();
        $scope = $parentScope.$new();
        $controller('LocationblacklistCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/BLACKLISTS.LOCATIONS.REASONS.json')
            .respond('{}');
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
        $httpBackend.whenGET('/blacklist/blacklists.json?limit=10&offset=0&type=room')
            .respond('{}');
    });

    it('instantiates and provides the states', function () {
        expect($scope.states).toBeDefined();
    });

    describe('when editing', function () {
        beforeEach(function () {
            $scope.action = 'edit';
            Modal.$scope = { resourceId: '241' };
        });

        it('fetches location blacklist data', function () {
            $httpBackend.expectGET('/blacklist/241?type=room')
                .respond(JSON.stringify(getJSONFixture('blacklist/241')));
            $controller('LocationblacklistCtrl', {$scope: $scope});
            $httpBackend.flush();
        });

        it('populates user data', function () {
            $httpBackend.whenGET('/blacklist/241?type=room')
                .respond(JSON.stringify(getJSONFixture('blacklist/241')));
            $controller('LocationblacklistCtrl', {$scope: $scope});
            $httpBackend.flush();

            expect($scope.states[0].stateController.ud.items.length).toBe(1);
            expect($scope.states[1].stateController.ud.fromDate).toBeDefined();
            expect($scope.states[2].stateController.ud.reason).toBeDefined();
        });
    });

    describe('#submit', function () {
        beforeEach(function () {
            Modal.$scope = { close: jasmine.createSpy() };
            $httpBackend.whenGET('/admin-ng/capture-agents/agents.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));

            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');

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
                $httpBackend.expectPOST('/blacklist').respond(201);
                $scope.submit();
                $httpBackend.flush();
            });

            describe('on success', function () {
                beforeEach(function () {
                    $httpBackend.whenPOST('/blacklist').respond(201);
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
                    $httpBackend.whenPOST('/blacklist').respond(500);
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
                $httpBackend.expectPUT('/blacklist')
                    .respond(JSON.stringify(getJSONFixture('blacklist/241')));
                $scope.submit();
                $httpBackend.flush();
            });

            describe('on success', function () {
                beforeEach(function () {
                    $httpBackend.whenPUT('/blacklist')
                        .respond(JSON.stringify(getJSONFixture('blacklist/241')));
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
                    $httpBackend.whenPUT('/blacklist').respond(500);
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
