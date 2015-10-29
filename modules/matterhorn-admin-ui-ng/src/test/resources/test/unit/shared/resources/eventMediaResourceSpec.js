describe('Event Media API Resource', function () {
    var EventMediaResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventMediaResource_) {
        $httpBackend  = _$httpBackend_;
        EventMediaResource = _EventMediaResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, mediaResponse = [{
                id: 'title'
            }, {
                id: 'series'
            }];
            $httpBackend.expectGET('/admin-ng/event/40518/asset/media/media.json').respond(JSON.stringify(mediaResponse));
            result = EventMediaResource.get({ id0: 40518 });
            $httpBackend.flush();
            expect(result.length).toBe(2);
        });
    });

});
