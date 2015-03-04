describe('Event Media Details API Resource', function () {
    var $httpBackend, EventMediaDetailsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventMediaDetailsResource_) {
        $httpBackend  = _$httpBackend_;
        EventMediaDetailsResource = _EventMediaDetailsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/events/40518/media/track-1.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/media/track-1.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/media/track-1.json')
                .respond(getJSONFixture('admin-ng/event/40518/media/track-1.json'));
            EventMediaDetailsResource.get({ id0: '40518', id1: 'track-1' });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/media/track-1.json')
                .respond(getJSONFixture('admin-ng/event/40518/media/track-1.json'));
            var data = EventMediaDetailsResource.get({ id0: '40518', id1: 'track-1' });
            $httpBackend.flush();
            expect(data.id).toEqual('370597b6-35ec-45de-9101-ccb87b873ee7');
        });
    });
});
