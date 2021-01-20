describe('AuthService', function () {
    var $httpBackend, AuthService;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));

    beforeEach(inject(function (_$httpBackend_, _AuthService_) {
        $httpBackend = _$httpBackend_;

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));

        AuthService = _AuthService_;
    }));

    it('instantiates', function () {
        expect(AuthService.user).toBeDefined();
    });

    describe('#getUser', function () {

        it('fetches authentication information from the server', function () {
            var user = AuthService.getUser();
            expect(user.username).toBeUndefined();
            $httpBackend.flush();
            expect(user.user.name).toEqual('Oliver Queen');
        });
    });

    describe('#userIsAuthorizedAs', function () {

        describe('without a user', function () {

            it('returns false', function () {
                expect(AuthService.userIsAuthorizedAs('ADMIN')).toBe(false);
            });
        });

        describe('without authorization', function () {
            beforeEach(function () {
                AuthService.user = {
                    username: 'Foo Bar',
                    roles: ['USER']
                };
            });

            it('returns false', function () {
                expect(AuthService.userIsAuthorizedAs('ADMIN')).toBe(false);
            });
        });

        describe('with authorization', function () {
            beforeEach(function () {
                AuthService.user = {
                    username: 'Foo Bar',
                    roles: ['USER', 'ADMIN']
                };
            });

            it('returns false', function () {
                expect(AuthService.userIsAuthorizedAs('ADMIN')).toBe(true);
            });
        });
    });

});
