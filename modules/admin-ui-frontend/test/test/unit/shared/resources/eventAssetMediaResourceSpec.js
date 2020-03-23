describe('Event Asset Media API Resource', function () {
    var EventAssetMediaResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventAssetMediaResource_) {
        $httpBackend  = _$httpBackend_;
        EventAssetMediaResource = _EventAssetMediaResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, mediaResponse = [{
                id: 'title'
            }, {
                id: 'series'
            }];
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/asset/media/media.json').respond(JSON.stringify(mediaResponse));
            result = EventAssetMediaResource.get({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' });
            $httpBackend.flush();
            expect(result.length).toBe(2);
        });
    });

});
