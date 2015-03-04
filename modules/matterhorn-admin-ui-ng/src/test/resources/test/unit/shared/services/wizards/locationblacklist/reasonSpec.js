describe('Reason Step in Location Blacklist Wizard', function () {
    var NewLocationblacklistReason, $httpBackend;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', { configureFromServer: function () {} });
    }));

    beforeEach(inject(function (_NewLocationblacklistReason_, _$httpBackend_) {
        NewLocationblacklistReason = _NewLocationblacklistReason_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/BLACKLISTS.LOCATIONS.REASONS.json')
        .respond(JSON.stringify(getJSONFixture('admin-ng/resources/BLACKLISTS.LOCATIONS.REASONS.json')));
    });

    it('fetches reasons', function () {
        expect(NewLocationblacklistReason.reasons.$resolved).toBe(false);
        $httpBackend.flush();
        expect(NewLocationblacklistReason.reasons.$resolved).toBe(true);
    });

    describe('#isValid', function () {

        it('is valid when the reason is set', function () {
            expect(NewLocationblacklistReason.isValid()).toBe(false);

            NewLocationblacklistReason.ud.reason = 'Excuse';

            expect(NewLocationblacklistReason.isValid()).toBe(true);
        });
    });
});
