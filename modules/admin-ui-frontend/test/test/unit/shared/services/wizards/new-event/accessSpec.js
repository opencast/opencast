describe('Access Step in New Event Wizard', function () {
    var NewEventAccess;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewEventAccess_) {
        NewEventAccess = _NewEventAccess_;
    }));

    describe('Access state', function () {
        it('accepts', function () {
            expect(NewEventAccess.isValid()).toBeFalsy();
        });
    });
});
