describe('Users API Resource', function () {
    var UsersResource, $httpBackend;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate:     function (val, date) { return date; },
            formatDateTime: function (val, date) { return date; },
            formatTime:     function (val, time) { return time; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _UsersResource_) {
        $httpBackend  = _$httpBackend_;
        UsersResource = _UsersResource_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#query', function () {

        it('calls the users service', function () {
            $httpBackend.expectGET('/admin-ng/users/users.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
            UsersResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/users/users.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
            var data = UsersResource.query();

            $httpBackend.flush();
            expect(data.rows.length).toBe(5);
            expect(data.rows[0].name).toBe('•mock• MH System Account');
            expect(data.rows[0].username).toBe('matterhorn_system_account');
            expect(data.rows[0].roles).toContain('ROLE_USER');
        });
    });
});
