describe('adminNg.filters.humanDuration', function () {

    beforeEach(module('adminNg'));

    it('has this filter', inject(function($filter) {
        expect($filter('humanDuration')).not.toBeNull();
    }));

    it('should echo for non-numbers', inject(function (humanDurationFilter) {
        expect(humanDurationFilter(true)).toBeTruthy();

        var nonNumbers = ['any string', {}];
        angular.forEach(nonNumbers, function(any) {
            expect(humanDurationFilter(any)).toBe(any);
        });

    }));

    it('should format correctly', inject(function (humanDurationFilter) {

        expect(humanDurationFilter(0)).toBe('0:00');
        expect(humanDurationFilter([1234000])).toBe('20:34'); // best effort
        expect(humanDurationFilter(1231231)).toBe('20:31');
        expect(humanDurationFilter('1231231')).toBe('20:31');
        expect(humanDurationFilter(1231231.1212)).toBe('20:31');
        expect(humanDurationFilter(3000.1212)).toBe('0:03');
        expect(humanDurationFilter(213000.12012)).toBe('3:33');
        expect(humanDurationFilter(18588000)).toBe('5:09:48');
        expect(humanDurationFilter(5438588)).toBe('1:30:38');
        expect(humanDurationFilter(3600000)).toBe('1:00:00');
        expect(humanDurationFilter(3200)).toBe('0:03');

    }));
});
