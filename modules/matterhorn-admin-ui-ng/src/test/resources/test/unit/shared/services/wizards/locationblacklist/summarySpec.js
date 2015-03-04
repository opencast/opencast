describe('Summary Step in Location Blacklist Wizard', function () {
    var NewLocationblacklistSummary;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewLocationblacklistSummary_) {
        NewLocationblacklistSummary = _NewLocationblacklistSummary_;
    }));

    describe('#isValid', function () {

        it('is valid by default', function () {
            expect(NewLocationblacklistSummary.isValid()).toBe(true);
        });
    });
});
