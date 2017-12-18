describe('Event Catalogs API Resource', function () {
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
            $httpBackend.whenGET('/admin-ng/event/30112/asset/publication/publications.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/publication/publications.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/publication/publications.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/publication/publications.json')));
            EventPublicationsResource.get({ id0: '30112'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/publication/publications.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/publication/publications.json')));
            var data = EventPublicationsResource.get({ id0: '30112' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.length).toEqual(1);
        });
    });
});
