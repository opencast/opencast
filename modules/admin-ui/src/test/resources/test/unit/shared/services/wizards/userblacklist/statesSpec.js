describe('States for the User Blacklist Wizard', function () {
    var NewUserblacklistStates;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewUserblacklistStates_) {
        NewUserblacklistStates = _NewUserblacklistStates_;
    }));

    it('provides states', function () {
        expect(NewUserblacklistStates.get().length).toBeGreaterThan(3);
    });
});
