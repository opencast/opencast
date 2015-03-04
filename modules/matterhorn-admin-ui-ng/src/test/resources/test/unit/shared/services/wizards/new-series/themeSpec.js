describe('Theme selection for the New Series Wizard', function () {
   var NewSeriesTheme;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_NewSeriesTheme_) {
        NewSeriesTheme = _NewSeriesTheme_;
    }));

    describe('#isValid', function () {

        it('returns true if theme is not selected (theme is optional)', function () {

            expect(NewSeriesTheme.isValid()).toEqual(true);
        });

        it('returns true if theme selected', function () {
            NewSeriesTheme.ud.theme = 'selected theme';

            expect(NewSeriesTheme.isValid()).toEqual(true);
        });
    });
});
