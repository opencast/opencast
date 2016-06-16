describe('ConstantsService', function () {
    var $httpBackend, ConstantsService;

    beforeEach(module('adminNg.services'));

    beforeEach(inject(function (_$httpBackend_, _ConstantsService_) {
        $httpBackend = _$httpBackend_;
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond(JSON.stringify({
            'keyA': 'Value A'
        }));
        ConstantsService = _ConstantsService_;
    }));

    it('provides a constructor', function () {
        expect(ConstantsService).toBeDefined();
    });

    it('fetches constants from the server', function () {
        $httpBackend.flush();
        expect(ConstantsService.keyA).toEqual('Value A');
    });
});
