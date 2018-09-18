describe('Event Asset Publications API Resource', function () {
    var $httpBackend, EventAssetPublicationsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventAssetPublicationsResource_) {
        $httpBackend  = _$httpBackend_;
        EventAssetPublicationsResource = _EventAssetPublicationsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json')));
            EventAssetPublicationsResource.get({ id0: '1a2a040b-ef73-4323-93dd-052b86036b75'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json')));
            var data = EventAssetPublicationsResource.get({ id0: '1a2a040b-ef73-4323-93dd-052b86036b75' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.length).toEqual(1);
        });
    });
});
