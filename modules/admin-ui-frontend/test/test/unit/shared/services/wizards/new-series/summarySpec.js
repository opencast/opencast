describe('Summary Step in New Series Wizard', function () {
    var NewSeriesSummary;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewSeriesSummary_) {
        NewSeriesSummary = _NewSeriesSummary_;
    }));


    it('accepts', function () {
        expect(NewSeriesSummary.isValid()).toBe(true);
    });
});
