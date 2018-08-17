describe('Summary Step for the Email Template Wizard', function () {
    var EmailtemplateSummary;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_EmailtemplateSummary_) {
        EmailtemplateSummary = _EmailtemplateSummary_;
    }));

    describe('#isValid', function () {

        it('is valid by default', function () {
            expect(EmailtemplateSummary.isValid()).toBe(true);
        });
    });
});
