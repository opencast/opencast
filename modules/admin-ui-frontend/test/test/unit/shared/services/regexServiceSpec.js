describe('RegexService', function () {

    var RegexService;

    beforeEach(module('adminNg.services'));

    beforeEach(inject(function (_$httpBackend_, _RegexService_) {
        RegexService = _RegexService_;
    }));

    it('transforms YYYY-MM-DD to a valid regex', function(){
        var regex = new RegExp(RegexService.translateDateFormatToPattern('YYYY-MM-DD'));
        expect(regex.test('2014-09-30')).toBe(true);
    });

});