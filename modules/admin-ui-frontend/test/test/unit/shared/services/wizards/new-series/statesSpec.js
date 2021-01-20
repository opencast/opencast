describe('States for the New Series Wizard', function () {
    var NewSeriesStates;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewSeriesStates_) {
        NewSeriesStates = _NewSeriesStates_;
    }));

    it('provides states', function () {
        expect(NewSeriesStates.get().length).toBe(5);
    });
});
