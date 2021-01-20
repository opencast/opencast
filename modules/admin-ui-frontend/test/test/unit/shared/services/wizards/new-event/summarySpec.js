describe('Summary Step in New Event Wizard', function () {
    var NewEventSummary;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewEventSummary_) {
        NewEventSummary = _NewEventSummary_;
    }));


    it('accepts', function () {
        expect(NewEventSummary.isValid()).toBe(true);
    });
});
