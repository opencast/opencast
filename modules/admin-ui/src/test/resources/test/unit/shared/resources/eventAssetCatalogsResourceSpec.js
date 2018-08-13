describe('Event Asset Catalogs API Resource', function () {
    var $httpBackend, EventAssetCatalogsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventAssetCatalogsResource_) {
        $httpBackend  = _$httpBackend_;
        EventAssetCatalogsResource = _EventAssetCatalogsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/30112/asset/catalog/catalogs.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/catalog/catalogs.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/catalog/catalogs.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/catalog/catalogs.json')));
            EventAssetCatalogsResource.get({ id0: '30112'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/catalog/catalogs.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/catalog/catalogs.json')));
            var data = EventAssetCatalogsResource.get({ id0: '30112' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.length).toEqual(1);
        });
    });
});
