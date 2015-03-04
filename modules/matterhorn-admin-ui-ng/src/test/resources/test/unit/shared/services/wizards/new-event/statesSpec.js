describe('States for the New Event Wizard', function () {
    var NewEventStates;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewEventStates_) {
        NewEventStates = _NewEventStates_;
    }));

    it('provides states', function () {
        expect(NewEventStates.get().length).toBeGreaterThan(3);
    });
});
