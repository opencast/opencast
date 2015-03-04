describe('Application controller', function () {
    var $scope, $controller, $httpBackend, $location, AuthService, Notifications, ResourceModal;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {}
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$location_, _$httpBackend_, _AuthService_, _Notifications_, _ResourceModal_) {
        $httpBackend = _$httpBackend_;
        $controller = _$controller_;
        $location = _$location_;
        AuthService = _AuthService_;
        Notifications = _Notifications_;
        ResourceModal = _ResourceModal_;

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').respond(
            JSON.stringify(getJSONFixture('info/me.json'))
        );
        $scope = $rootScope.$new();

        $controller('ApplicationCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.currentUser).toBeDefined();
    });

    it('stores the current user', function () {
        $httpBackend.flush();

        expect($scope.currentUser.user.name).toEqual('Oliver Queen');
    });

    describe('with a modal specified in the URL', function () {
        beforeEach(function () {
            $location.search({
                modal: 'modalA',
                resourceId: '491',
                tab: 'tabB',
                action: 'edit'
            });
            spyOn(ResourceModal, 'show');
            $controller('ApplicationCtrl', {$scope: $scope});
        });

        it('opens a resource modal', function () {
            expect(ResourceModal.show).toHaveBeenCalledWith('modalA', '491', 'tabB', 'edit');
        });
    });
});
