describe('Summary Step in User Blacklist Wizard', function () {
    var NewUserblacklistSummary;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewUserblacklistSummary_) {
        NewUserblacklistSummary = _NewUserblacklistSummary_;
    }));

    describe('#isValid', function () {

        it('is valid by default', function () {
            expect(NewUserblacklistSummary.isValid()).toBe(true);
        });
    });
});
