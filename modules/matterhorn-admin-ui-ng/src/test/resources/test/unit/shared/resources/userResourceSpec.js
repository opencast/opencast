describe('User API Resource', function () {
    var $httpBackend, UserResource, CryptService;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('angular-md5'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _UserResource_, _CryptService_) {
        $httpBackend  = _$httpBackend_;
        UserResource = _UserResource_;
        CryptService = _CryptService_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#get', function () {

        it('queries the user API', function () {
            $httpBackend.expectGET('/admin-ng/users/admin.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/users/admin.json')));
            UserResource.get({ username: 'admin' });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.whenGET('/admin-ng/users/admin.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/users/admin.json')));
            var data = UserResource.get({ username: 'admin' });
            $httpBackend.flush();
            expect(data.name).toEqual('•mock• SystemAdmin');
        });
    });
});
