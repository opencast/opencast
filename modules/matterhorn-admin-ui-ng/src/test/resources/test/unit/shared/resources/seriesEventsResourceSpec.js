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
            $httpBackend.expectGET('/admin-ng/series/40518/events.json')
                .respond(JSON.stringify(eventsResponse));
            result = SeriesEventsResource.get({ id: 40518 });
            $httpBackend.flush();
            expect(result.entries.length).toBe(2);
        });
    });

    describe('#save', function () {
        it('sends an array of events', function () {
            var eventsRequest = {
                entries: [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPUT('/admin-ng/series/40518/events.json', eventsRequest.entries)
                .respond(200);
            SeriesEventsResource.save({ id: '40518' }, eventsRequest);
            $httpBackend.flush();
        });
    });
});
