describe('Series Participation API Resource', function () {
    var SeriesParticipationResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _SeriesParticipationResource_) {
        $httpBackend  = _$httpBackend_;
        SeriesParticipationResource = _SeriesParticipationResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, metadataResponse = {
              'opt_out': true,
              'review_status': 'Unsent', 
              'messages': [
                {
                  'id': '12',
                  'subject': 'Message subject',
                  'creator': 'George',
                  'creation_date': '12.12.12'
                }]
            };
            $httpBackend.expectGET('/admin-ng/series/40518/participation.json').respond(JSON.stringify(metadataResponse));
            result = SeriesParticipationResource.get({ id: 40518 });
            $httpBackend.flush();

            expect(result.opt_out).toEqual('' + metadataResponse.opt_out);
            expect(result.review_status).toEqual('' + metadataResponse.review_status);
            expect(result.messages).toEqual(metadataResponse.messages);
        });
    });
});
