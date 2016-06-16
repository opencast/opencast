describe('Reason Step in User Blacklist Wizard', function () {
    var NewUserblacklistReason, $httpBackend;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', { configureFromServer: function () {} });
    }));

    beforeEach(inject(function (_NewUserblacklistReason_, _$httpBackend_) {
        NewUserblacklistReason = _NewUserblacklistReason_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/BLACKLISTS.USERS.REASONS.json')
        .respond(JSON.stringify(getJSONFixture('admin-ng/resources/BLACKLISTS.USERS.REASONS.json')));
    });

    it('fetches reasons', function () {
        expect(NewUserblacklistReason.reasons.$resolved).toBe(false);
        $httpBackend.flush();
        expect(NewUserblacklistReason.reasons.$resolved).toBe(true);
    });

    describe('#isValid', function () {

        it('is valid when the reason is set', function () {
            expect(NewUserblacklistReason.isValid()).toBe(false);

            NewUserblacklistReason.ud.reason = 'Excuse';

            expect(NewUserblacklistReason.isValid()).toBe(true);
        });
    });
});
