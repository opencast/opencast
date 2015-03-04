describe('States for the Bulk Message Wizard', function () {
    var BulkMessageStates;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_BulkMessageStates_) {
        BulkMessageStates = _BulkMessageStates_;
    }));

    it('provides states', function () {
        expect(BulkMessageStates.get().length).toBeGreaterThan(1);
    });
});
