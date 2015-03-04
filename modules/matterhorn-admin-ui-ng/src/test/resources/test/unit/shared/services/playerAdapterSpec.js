describe('PlayerAdapter', function () {
    var $httpBackend, PlayerAdapter;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));

    beforeEach(inject(function (_$httpBackend_, _PlayerAdapter_) {
        $httpBackend = _$httpBackend_;

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));

        PlayerAdapter = _PlayerAdapter_;
    }));

    it('instantiates', function () {
        expect(PlayerAdapter.STATUS).toBeDefined();
        expect(PlayerAdapter.EVENTS).toBeDefined();
    });

    describe('#eventMapping', function () {
        var mapping;

        beforeEach(function () {
            mapping = PlayerAdapter.eventMapping();
            mapping.map(PlayerAdapter.EVENTS.TIMEUPDATE, 'timeupdate');
        });

        it('translates event names', function () {
            expect(mapping.resolveNativeName('timeupdate'))
                .toEqual(PlayerAdapter.EVENTS.TIMEUPDATE);
        });
    });
});
