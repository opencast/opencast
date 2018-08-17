describe('User Preferences controller', function () {
    var $scope, $httpBackend, Language;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {}
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _Language_) {
        Language = _Language_;
        $httpBackend = _$httpBackend_;

        $scope = $rootScope.$new();
        $controller('UserPreferencesCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';

        $httpBackend.whenGET('/admin-ng/user-settings/signature')
            .respond(JSON.stringify(getJSONFixture('admin-ng/user-settings/signature')));
        $httpBackend.whenGET('/info/me.json')
            .respond(JSON.stringify(getJSONFixture('info/me.json')));

        $scope.userPrefForm = {};
        $scope.close = jasmine.createSpy();
    });

    it('instantiates', function () {
        expect($scope.userprefs).toBeDefined();
    });

    describe('#update', function () {

        describe('with a valid form', function () {
            beforeEach(function () {
                $scope.userPrefForm.$valid = true;
            });

            it('persists the signature', function () {
                $httpBackend.expectPUT('/admin-ng/user-settings/signature/1501')
                    .respond(200);
                $scope.update();
                $httpBackend.flush();

                expect($scope.close).toHaveBeenCalled();
            });
        });

        describe('with an invalid form', function () {
            beforeEach(function () {
                $scope.userPrefForm.$valid = false;
            });

            it('does not persist', function () {
                $scope.update();
                $httpBackend.flush();

                expect($scope.close).not.toHaveBeenCalled();
            });
        });
    });

    describe('#save', function () {

        describe('with a valid form', function () {
            beforeEach(function () {
                $scope.userPrefForm.$valid = true;
            });

            it('persists the signature', function () {
                $httpBackend.expectPOST('/admin-ng/user-settings/signature/1501')
                    .respond(200);
                $scope.save();
                $httpBackend.flush();

                expect($scope.close).toHaveBeenCalled();
            });
        });

        describe('with an invalid form', function () {
            beforeEach(function () {
                $scope.userPrefForm.$valid = false;
            });

            it('does not persist', function () {
                $scope.save();
                $httpBackend.flush();

                expect($scope.close).not.toHaveBeenCalled();
            });
        });
    });
});
