describe('Event Asset Attachments API Resource', function () {
    var $httpBackend, EventAssetAttachmentsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventAssetAttachmentsResource_) {
        $httpBackend  = _$httpBackend_;
        EventAssetAttachmentsResource = _EventAssetAttachmentsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/30112/asset/attachment/attachments.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/attachment/attachments.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/attachment/attachments.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/attachment/attachments.json')));
            EventAssetAttachmentsResource.get({ id0: '30112'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/30112/asset/attachment/attachments.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/30112/asset/attachment/attachments.json')));
            var data = EventAssetAttachmentsResource.get({ id0: '30112' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.length).toEqual(1);
        });
    });
});
