describe('States for the Email Template Wizard', function () {
    var EmailtemplateStates;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_EmailtemplateStates_) {
        EmailtemplateStates = _EmailtemplateStates_;
    }));

    it('provides states', function () {
        expect(EmailtemplateStates.get().length).toBeGreaterThan(1);
    });
});
