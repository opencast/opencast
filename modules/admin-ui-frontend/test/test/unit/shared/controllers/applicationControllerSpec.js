describe('Application controller', function () {
    var $scope, $controller, $httpBackend, $location, AuthService, Notifications,
      ResourceModal, AdopterRegistrationResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {}
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$location_,
                                _$httpBackend_, _AuthService_, _Notifications_,
                                _ResourceModal_, _AdopterRegistrationResource_) {

        $httpBackend = _$httpBackend_;
        $controller = _$controller_;
        $location = _$location_;
        AuthService = _AuthService_;
        Notifications = _Notifications_;
        ResourceModal = _ResourceModal_;
        AdopterRegistrationResource = _AdopterRegistrationResource_;

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').respond(
            JSON.stringify(getJSONFixture('info/me.json'))
        );

        $httpBackend.whenGET('/admin-ng/adopter/registration').respond(
            {'lastModified' : '2020.01.16'}
        );

        $httpBackend.whenGET('/sysinfo/bundles/version?prefix=opencast').respond(
            {'buildNumber': '01b60ff', 'consistent': true, 'version': '1.6.0.SNAPSHOT'}
        );
        $httpBackend.whenGET('oc-version/version.json').respond({'data': '1.6.0SNAPSHOT'});
        $httpBackend.whenGET('/broker/status').respond('{}');
        $httpBackend.whenGET('/services/services.json').respond(
                   {"services":
                     {"service":[
                      {"type":"fake","host":"localhost","path":"\/fake","active":true,"online":true,"maintenance":false,
                       "jobproducer":false,"onlinefrom":"2017-08-24T10:33:29-06:00","service_state":"NORMAL",
                       "state_changed":"2017-08-24T10:33:29-06:00","error_state_trigger":0,"warning_state_trigger":0}
                     ]}
                   });
        $httpBackend.whenGET('/services/health.json').respond('{}');
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $httpBackend.whenGET('shared/partials/modals/registration-modal.html').respond('');

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

    describe('version', function () {

        it('sets the version out of object', function () {
            $httpBackend.whenGET('/sysinfo/bundles/version?prefix=opencast').respond(
                {'buildNumber': '01b60ff', 'consistent': true, 'version': '1.6.0.SNAPSHOT'}
            );

            $httpBackend.flush();
            expect($scope.version.version).toEqual('1.6.0.SNAPSHOT');
        });

        it('sets the version out of array', function () {
            $httpBackend.whenGET('/sysinfo/bundles/version?prefix=opencast').respond(
                {versions: [{'buildNumber': '01b60ff', 'consistent': true, 'version': '1.6.0.SNAPSHOT'}]}
            );

            $httpBackend.flush();
            expect($scope.version.version).toEqual('1.6.0.SNAPSHOT');
        });

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
