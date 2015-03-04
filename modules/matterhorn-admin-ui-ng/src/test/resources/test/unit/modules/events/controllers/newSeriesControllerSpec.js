describe('New Series Controller', function () {
    var $scope, $parentScope, $httpBackend, Modal, Table, Notifications;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _Modal_, _Table_, _Notifications_) {
        Notifications = _Notifications_;
        Modal = _Modal_;
        Table = _Table_;
        $httpBackend = _$httpBackend_;

        $parentScope = $rootScope.$new();
        $scope = $parentScope.$new();
        $controller('NewSeriesCtrl', {$scope: $scope});
    }));

    it('instantiates and provides the states', function () {
        expect($scope.states).toBeDefined();
    });

    describe('#submit', function () {
        beforeEach(function () {
            $httpBackend.whenGET('/admin-ng/resources/THEMES.NAME.json').respond('{}');
            $httpBackend.whenGET('/admin-ng/series/new/metadata').respond('{}');
            $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond('{}');
            $httpBackend.whenGET('/admin-ng/resources/ROLES.json').respond('{}');
            $httpBackend.whenGET('/admin-ng/resources/components.json').respond('{}');
            $httpBackend.whenGET('/admin-ng/series/new/themes').respond('{}');

            Modal.$scope = { close: jasmine.createSpy() };
            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');

            $scope.states = [{
                name: 'metadata',
                stateController: { ud: { metadata: { title: 'meta data' } } }
            }, {
                name: 'access',
                stateController: { ud: { id: 345 } }
            }, {
                name: 'theme',
                stateController: { ud: { theme: '2' } }
            }];
        });

        it('saves collected userdata', function () {
            $httpBackend.expectPOST('/admin-ng/series/new', function (data) {
                expect(angular.fromJson($.deparam(data).metadata)).toEqual({
                    metadata: [{ 'title': 'meta data' }],
                    options: {},
                    access: { acl: { ace: []} },
                    theme: 2
                });
                return true;
            }).respond(200);
            $scope.submit();
            $httpBackend.flush();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenPOST('/admin-ng/series/new').respond(201);
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
                $httpBackend.whenPOST('/admin-ng/series/new').respond(500);

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
