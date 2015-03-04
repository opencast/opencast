describe('Summary Step for the Bulk Message Wizard', function () {
    var BulkMessageSummary;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_BulkMessageSummary_) {
        BulkMessageSummary = _BulkMessageSummary_;
    }));

    describe('#isValid', function () {

        it('is valid by default', function () {
            expect(BulkMessageSummary.isValid()).toBe(true);
        });
    });
});
