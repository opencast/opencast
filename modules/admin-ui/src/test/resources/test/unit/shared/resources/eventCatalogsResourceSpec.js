describe('Event Catalogs API Resource', function () {
    var $httpBackend, EventCatalogsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventCatalogsResource_) {
        $httpBackend  = _$httpBackend_;
        EventCatalogsResource = _EventCatalogsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/30112/asset/catalog/catalogs.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/catalog/catalogs.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/catalog/catalogs.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/catalog/catalogs.json')));
            EventCatalogsResource.get({ id0: '30112'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/catalog/catalogs.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/catalog/catalogs.json')));
            var data = EventCatalogsResource.get({ id0: '30112' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.length).toEqual(1);
        });
    });
});
