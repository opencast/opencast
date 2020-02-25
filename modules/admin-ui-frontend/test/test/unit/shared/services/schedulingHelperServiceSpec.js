describe('SchedulingHelperService', function () {
    var $httpBackend, SchedulingHelperService;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));

    beforeEach(inject(function (_SchedulingHelperService_) {
        SchedulingHelperService = _SchedulingHelperService_;
    }));

    it('parses date strings correctly', function () {
        expect(SchedulingHelperService.parseDate("2018-01-20", 10, 25)).toEqual(new Date(2018, 0, 20, 10, 25));
    });

    describe('#applyTemporalValueChange', function () {
        var temporalValues;

        beforeEach(function () {
            temporalValues = {
                start: {
                    date: "2018-01-20",
                    hour: 10,
                    minute: 25
                },
                end: {
                    date: "2018-01-20",
                    hour: 12,
                    minute: 40
                },
                duration: {
                    hour: 3,
                    minute: 2
                }
            };
        });

        it('updates the duration when the start time is changed', function() {
            var expectedTemporalValues = angular.copy(temporalValues);
            expectedTemporalValues.duration = {hour: 2, minute: 15};
            SchedulingHelperService.applyTemporalValueChange(temporalValues, 'start', true);
            expect(temporalValues).toEqual(expectedTemporalValues);
        });

        it('updates the duration when the end time is changed', function() {
            var expectedTemporalValues = angular.copy(temporalValues);
            expectedTemporalValues.duration = {hour: 2, minute: 15};
            SchedulingHelperService.applyTemporalValueChange(temporalValues, 'end', true);
            expect(temporalValues).toEqual(expectedTemporalValues);
        });

        it('updates the end time when the duration is changed', function() {
            var expectedTemporalValues = angular.copy(temporalValues);
            expectedTemporalValues.end.hour = 13;
            expectedTemporalValues.end.minute = 27;
            SchedulingHelperService.applyTemporalValueChange(temporalValues, 'duration', true);
            expect(temporalValues).toEqual(expectedTemporalValues);
        });

        it('updates the end date when the duration is changed and the recording lasts until the next day', function() {
            temporalValues.duration.hour = 23;
            var expectedTemporalValues = angular.copy(temporalValues);
            expectedTemporalValues.end.date = "2018-01-21";
            expectedTemporalValues.end.hour = 9;
            expectedTemporalValues.end.minute = 27;
            SchedulingHelperService.applyTemporalValueChange(temporalValues, 'duration', true);
            expect(temporalValues).toEqual(expectedTemporalValues);
        });

        it('updates the end date when the end time is changed and the recording lasts until the next day', function() {
            temporalValues.end.hour = 9;
            var expectedTemporalValues = angular.copy(temporalValues);
            expectedTemporalValues.end.date = "2018-01-21";
            expectedTemporalValues.duration = {hour: 23, minute: 15};
            SchedulingHelperService.applyTemporalValueChange(temporalValues, 'end', true);
            expect(temporalValues).toEqual(expectedTemporalValues);
        });

        it('does not touch the end date when scheduling multiple events', function() {
            temporalValues.duration.hour = 23;
            var expectedTemporalValues = angular.copy(temporalValues);
            expectedTemporalValues.end.date = "2018-01-20";
            expectedTemporalValues.end.hour = 9;
            expectedTemporalValues.end.minute = 27;
            SchedulingHelperService.applyTemporalValueChange(temporalValues, 'duration', false);
            expect(temporalValues).toEqual(expectedTemporalValues);
        });

    });

});
