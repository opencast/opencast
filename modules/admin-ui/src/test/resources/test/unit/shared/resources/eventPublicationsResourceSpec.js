describe('Event Publications API Resource', function () {
    var $httpBackend, EventPublicationsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventPublicationsResource_) {
        $httpBackend  = _$httpBackend_;
        EventPublicationsResource = _EventPublicationsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/40518/publications.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/publications.json')));
        });

        it('queries the publications API', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/publications.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/publications.json')));
            EventPublicationsResource.get({ id: '40518'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.whenGET('/admin-ng/event/40518/publications.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/publications.json')));
            var data = EventPublicationsResource.get({ id: '40518' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.publications.length).toBe(1);
            expect(data.publications[0].name).toBe('engage-player');
        });
    });
});
