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
            $httpBackend.expectGET('/admin-ng/event/40518/asset/media/media.json').respond(JSON.stringify(mediaResponse));
            result = EventAssetMediaResource.get({ id0: 40518 });
            $httpBackend.flush();
            expect(result.length).toBe(2);
        });
    });

});
