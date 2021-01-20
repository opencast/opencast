describe('adminNg.filters.humanBytes', function () {

    beforeEach(module('adminNg'));

    it('has a humanBytes filter', inject(function($filter) {
        expect($filter('humanBytes')).not.toBeNull();
    }));

    it('should echo for non-numbers', inject(function (humanBytesFilter) {
        expect(humanBytesFilter(true)).toBeTruthy();

        var nonNumbers = ['any string', {}];
        angular.forEach(nonNumbers, function(any) {
            expect(humanBytesFilter(any)).toBe(any);
        });

    }));

    it('should format correctly', inject(function (humanBytesFilter) {

        expect(humanBytesFilter(1)).toBe('1 B');
        expect(humanBytesFilter(0.343)).toBe('0 B');
        expect(humanBytesFilter([1234])).toBe('1.2 kB');
        expect(humanBytesFilter(200.232)).toBe('200 B');
        expect(humanBytesFilter(9999999)).toBe('10.0 MB');
        expect(humanBytesFilter('9999999')).toBe('10.0 MB');

    }));
});
