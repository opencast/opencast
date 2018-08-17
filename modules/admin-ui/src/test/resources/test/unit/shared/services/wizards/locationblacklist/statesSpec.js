describe('States for the Location Blacklist Wizard', function () {
    var NewLocationblacklistStates;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewLocationblacklistStates_) {
        NewLocationblacklistStates = _NewLocationblacklistStates_;
    }));

    it('provides states', function () {
        expect(NewLocationblacklistStates.get().length).toBeGreaterThan(3);
    });
});
