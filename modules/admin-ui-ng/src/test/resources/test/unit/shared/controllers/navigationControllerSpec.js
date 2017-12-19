describe('Navigation controller', function () {
    var $scope, $httpBackend, Language;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            changeLanguage: function () {},
            getLanguageCode: function () { return 'ja_JP'; },
            getLanguage: function () { return {}; },
            getAvailableLanguages: function () { return []; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _Language_) {
        Language = _Language_;
        $httpBackend = _$httpBackend_;

        $httpBackend.whenGET('/info/me.json').respond('');

        $scope = $rootScope.$new();
        $controller('NavCtrl', {$scope: $scope});
    }));

    it('sets the language code when it changes', function () {
        $scope.$emit('language-changed');
        expect($scope.currentLanguageCode).toEqual('ja_JP');
    });

    describe('#changeLanguage', function () {

        it('sets the language on the language service', function () {
            spyOn(Language, 'changeLanguage');
            $scope.changeLanguage('de_DE');
            expect(Language.changeLanguage).toHaveBeenCalled();
        });
    });

    // Disable at the moment, the logout is done throug a redirection
    xdescribe('#logout', function () {
        beforeEach(function () {
            spyOn($scope.location, 'reload');
        });

        it('queries spring security', function () {
            $httpBackend.expectGET('/j_spring_security_logout').respond('');
            $scope.logout();
            $httpBackend.flush();
        });

        it('reloads the page', function () {
            $httpBackend.whenGET('/j_spring_security_logout').respond('');
            $scope.logout();
            $httpBackend.flush();

            expect($scope.location.reload).toHaveBeenCalled();
        });
    });
});
