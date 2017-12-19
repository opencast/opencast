describe('Event Metadata API Resource', function () {
    var EventMetadataResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventMetadataResource_) {
        $httpBackend  = _$httpBackend_;
        EventMetadataResource = _EventMetadataResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, metadataResponse = [{
                flavor: 'dublincore/episode',
                fields: [{ id: 'title' }, { id: 'series' }]
            },
            {
                flavor: 'dublincore/extended',
                fields: [{ id: 'title' }, { id: 'series' }]
            }];
            $httpBackend.expectGET('/admin-ng/event/40518/metadata.json').respond(JSON.stringify(metadataResponse));
            result = EventMetadataResource.get({ id: 40518 });
            $httpBackend.flush();
            expect(result.entries).toEqual(metadataResponse);
        });
    });

    describe('#save', function () {
        it('sends an array of metadata', function () {
            var metadataRequest = {
                fields: [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPUT('/admin-ng/event/40518/metadata', function (data) {
                var expected = [{ fields : [ { id : 'title' }, { id : 'series' } ] }];
                expect(angular.fromJson($.deparam(data).metadata)).toEqual(expected);
                return true;
            }).respond(200);
            EventMetadataResource.save({ id: '40518' }, metadataRequest);
            $httpBackend.flush();
        });

        it('sends only the changed attribute if asked to', function () {
            var metadataRequest = {
                attributeToSend : 'series',
                fields          : [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPUT('/admin-ng/event/40518/metadata', function (data) {
                var expected = [{fields: [{'id': 'series'}]}];
                expect(angular.fromJson($.deparam(data).metadata)).toEqual(expected);
                return true;
            }).respond(200);
            EventMetadataResource.save({ id: '40518' }, metadataRequest);
            $httpBackend.flush();
        });
    });
});
