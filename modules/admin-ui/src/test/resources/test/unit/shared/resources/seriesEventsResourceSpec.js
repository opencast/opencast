describe('Series Events API Resource', function () {
    var SeriesEventsResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _SeriesEventsResource_) {
        $httpBackend  = _$httpBackend_;
        SeriesEventsResource = _SeriesEventsResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, eventsResponse = [{
                id: 'title'
            }, {
                id: 'series'
            }];
            $httpBackend.expectGET('/admin-ng/series/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/events.json')
                .respond(JSON.stringify(eventsResponse));
            result = SeriesEventsResource.get({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' });
            $httpBackend.flush();
            expect(result.entries.length).toBe(2);
        });
    });

    describe('#save', function () {
        it('sends an array of events', function () {
            var eventsRequest = {
                entries: [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPUT('/admin-ng/series/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/events.json', eventsRequest.entries)
                .respond(200);
            SeriesEventsResource.save({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' }, eventsRequest);
            $httpBackend.flush();
        });
    });
});
