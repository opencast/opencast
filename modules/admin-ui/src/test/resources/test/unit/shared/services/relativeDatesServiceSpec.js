describe('RelativeDatesService', function () {

    var RelativeDatesService;

    beforeEach(module('adminNg.services'));

    beforeEach(inject(function (_RelativeDatesService_) {

        RelativeDatesService = _RelativeDatesService_;
    }));

    it('instantiates', function () {

        expect(RelativeDatesService.relativeToAbsoluteDate).toBeDefined();
        expect(RelativeDatesService.relativeDateSpanToFilterValue).toBeDefined();

        //TODO call to moment.locale
    });

    describe('relativeToAbsoluteDate', function () {

        it('returns beginning of today', function () {

            var relativeDate = RelativeDatesService.relativeToAbsoluteDate(0, 'day', true)
            var todayDate = new Date();
            todayDate.setHours(0, 0, 0, 0);

            expect(relativeDate).toBeDefined();
            expect(relativeDate).toEqual(todayDate);
        });

        it('returns end of yesterday', function () {

            var relativeDate = RelativeDatesService.relativeToAbsoluteDate(-1, 'day', false)
            var yesterdayDate = new Date();
            yesterdayDate.setDate(yesterdayDate.getDate() - 1);
            yesterdayDate.setHours(23, 59, 59, 999); //TODO

            expect(relativeDate).toBeDefined();
            expect(relativeDate).toEqual(yesterdayDate);
        });
    });

    describe('relativeDateSpanToFilterValue', function () {

        it('returns date span from beginning of yesterday to end of today', function () {

            var relativeDateSpan = RelativeDatesService.relativeDateSpanToFilterValue(-1, 0, 'day');

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
