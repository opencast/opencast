describe('Tools Edit controller', function () {
    var $scope, $controller, $parentScope, $location, $httpBackend, Notifications;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
        $provide.value('$route', {
            current: {
                params: {
                    itemId: 5681,
                    resource: 'events',
                    tab: 'editor'
                }
            }
        });
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$location_, _$httpBackend_, _Notifications_) {
        $controller = _$controller_;
        $location = _$location_;
        $httpBackend = _$httpBackend_;
        Notifications = _Notifications_;

        $parentScope = $rootScope.$new();
        $scope = $parentScope.$new();

        $controller('ToolsCtrl', { $scope: $scope });
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/tools/5681/editor.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/tools/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/editor.json')));
    });

    it('instantiates', function () {
        expect($scope.video).toBeDefined();
        $httpBackend.flush();
        expect($scope.video.duration).toBeDefined();
    });

    describe('#openTab', function () {

        it('navigates to the given tab', function () {
            $scope.openTab('foo-tab');
            expect($location.path()).toEqual('/events/events/5681/tools/foo-tab');
        });
    });

    describe('#submit', function () {
        beforeEach(function () {
            $httpBackend.flush();
            spyOn(Notifications, 'add');
            spyOn($location, 'url');
        });

        it('saves the cutting state', function () {
            $httpBackend.expectPOST('/admin-ng/tools/5681/editor.json').respond(201);
            $scope.submit();
            $httpBackend.flush();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenPOST('/admin-ng/tools/5681/editor.json').respond(201);
            });

            it('shows a notification when saving', function () {
                $scope.submit();
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
            });

            it('shows a notification when saving and processing', function () {
                $scope.video.workflow = 'some-workflow';
                $scope.submit();
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
            });

            it('redirects to the events list', function () {
                $scope.video.workflow = 'some-workflow';
                $scope.submit();
                $httpBackend.flush();

                expect($location.url).toHaveBeenCalledWith('/events/events');
            });
        });

        describe('on error', function () {
            beforeEach(function () {
                $httpBackend.whenPOST('/admin-ng/tools/5681/editor.json').respond(500);
            });

            it('shows a notification', function () {
                $scope.submit();
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String), jasmine.any(String));
            });
        });
    });
});
