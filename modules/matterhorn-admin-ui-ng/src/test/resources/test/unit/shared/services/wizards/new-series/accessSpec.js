describe('Access Step in New Series Wizard', function () {
    var NewSeriesAccess;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewSeriesAccess_) {
        NewSeriesAccess = _NewSeriesAccess_;
    }));

    describe('Access state', function () {
        it('accepts', function () {
            expect(NewSeriesAccess.isValid()).toBeFalsy();
        });
    });
});
