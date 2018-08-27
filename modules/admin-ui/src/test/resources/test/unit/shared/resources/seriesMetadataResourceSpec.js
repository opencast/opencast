describe('Series Metadata API Resource', function () {
    var SeriesMetadataResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _SeriesMetadataResource_) {
        $httpBackend  = _$httpBackend_;
        SeriesMetadataResource = _SeriesMetadataResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, metadataResponse = [{
                flavor: 'dublincore/series',
                fields: [{ id: 'title' }, { id: 'series' }]
            },
            {
                flavor: 'dublincore/extended',
                fields: [{ id: 'title' }, { id: 'series' }]
            }];
            $httpBackend.expectGET('/admin-ng/series/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/metadata.json').respond(JSON.stringify(metadataResponse));
            result = SeriesMetadataResource.get({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' });
            $httpBackend.flush();
            expect(result.entries).toEqual(metadataResponse);
        });
    });

    describe('#save', function () {
        it('sends an array of metadata', function () {
            var metadataRequest = {
                fields: [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPUT('/admin-ng/series/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/metadata', function (data) {
                expect($.deparam(data)).toEqual({
                    metadata: JSON.stringify([metadataRequest])
                });
                return true;
            }).respond(200);
            SeriesMetadataResource.save({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' }, metadataRequest);
            $httpBackend.flush();
        });

        it('sends only the changed attribute if asked to', function () {
            var metadataRequest = {
                attributeToSend : 'series',
                fields          : [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPUT('/admin-ng/series/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/metadata', function (data) {
                var expected = [{fields: [{'id': 'series'}]}];
                expect(angular.fromJson($.deparam(data).metadata)).toEqual(expected);
                return true;
            }).respond(200);
            SeriesMetadataResource.save({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' }, metadataRequest);
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.expectPUT('/admin-ng/series/8/metadata').respond(200);
            SeriesMetadataResource.save({id: 8}, {});
            $httpBackend.flush();
        });
    });
});
