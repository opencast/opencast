describe('RelativeDatesService', function () {

    var RelativeDatesService;

    beforeEach(module('adminNg.services'));

    beforeEach(inject(function (_RelativeDatesService_) {

        RelativeDatesService = _RelativeDatesService_;
    }));

    it('instantiates', function () {

        expect(RelativeDatesService.updateDefaultLocale).toBeDefined();
        expect(RelativeDatesService.relativeToAbsoluteDate).toBeDefined();
        expect(RelativeDatesService.relativeDateSpanToFilterValue).toBeDefined();
    });

    describe('updateDefaultLocale', function () {

        var __originalNavigator;

        beforeEach(function() {
            __originalNavigator = navigator;
            navigator = new Object();
            navigator.__proto__ = __originalNavigator;
        });

        afterEach(function() {
            navigator = __originalNavigator;
        })

        it('updates the default locale for german', function() {

            navigator.__defineGetter__('language', function () { return 'de-DE'; });

            RelativeDatesService.updateDefaultLocale();

            var defaultLocale = Sugar.Date.getLocale()
            expect(defaultLocale.code).toContain("-mod");

            expect(defaultLocale.firstDayOfWeek).toBeUndefined();
            expect(defaultLocale.firstDayOfWeekYear).toBeUndefined();

        });

        it('updates the default locale for japanese', function() {

            navigator.__defineGetter__('language', function () { return 'ja-JA'; });

            RelativeDatesService.updateDefaultLocale();

            var defaultLocale = Sugar.Date.getLocale()
            expect(defaultLocale.code).toContain("-mod");

            expect(defaultLocale.firstDayOfWeek).toBe(0);
            expect(defaultLocale.firstDayOfWeekYear).toBe(1);
        });
    });

    describe('relativeToAbsoluteDate', function () {

        it('returns today', function () {

            var relativeDate = RelativeDatesService.relativeToAbsoluteDate('today')
            var todayDate = new Date();
            todayDate.setHours(0, 0, 0, 0);

            expect(relativeDate).toBeDefined();
            expect(relativeDate).toEqual(todayDate);
        });
    });

    describe('relativeDateSpanToFilterValue', function () {

        it('returns date span from beginning of yesterday to end of today', function () {

            var relativeDateSpan = RelativeDatesService.relativeDateSpanToFilterValue('beginning of yesterday',
            'end of today')

            var todayDate = new Date();
            todayDate.setHours(23, 59, 59, 999);

            var yesterdayDate = new Date();
            yesterdayDate.setDate(yesterdayDate.getDate() - 1);
            yesterdayDate.setHours(0, 0, 0, 0);

            expect(relativeDateSpan).toBeDefined();
            expect(relativeDateSpan).toContain("/");

            var relativeDateSpanSplitted = relativeDateSpan.split("/");
            expect(relativeDateSpanSplitted.length == 2).toBe(true);

            var relativeDateFrom = relativeDateSpanSplitted[0];
            var relativeDateTo = relativeDateSpanSplitted[1];

            expect(relativeDateFrom).toBeDefined();
            expect(relativeDateTo).toBeDefined();

            expect(relativeDateFrom).toEqual(yesterdayDate.toISOString());
            expect(relativeDateTo).toEqual(todayDate.toISOString());
        });
    });

});
