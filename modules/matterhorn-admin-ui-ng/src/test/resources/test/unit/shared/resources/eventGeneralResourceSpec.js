describe('Event General API Resource', function () {
    var $httpBackend, EventGeneralResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventGeneralResource_) {
        $httpBackend  = _$httpBackend_;
        EventGeneralResource = _EventGeneralResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/40518/general.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/general.json')));
        });

        it('queries the general API', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/general.json').respond(getJSONFixture('admin-ng/event/40518/general.json'));
            EventGeneralResource.get({ id: '40518'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.whenGET('/admin-ng/event/40518/general.json').respond(getJSONFixture('admin-ng/event/40518/general.json'));
            var data = EventGeneralResource.get({ id: '40518' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.publications.length).toBe(1);
            expect(data.publications[0].name).toBe('engage-player');
        });
    });
});
