describe('User API Resource', function () {
    var $httpBackend, UserResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _UserResource_) {
        $httpBackend  = _$httpBackend_;
        UserResource = _UserResource_;
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
