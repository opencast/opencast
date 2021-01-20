describe('Identity API Resource', function () {
    var IdentityResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _IdentityResource_) {
        $httpBackend  = _$httpBackend_;
        IdentityResource = _IdentityResource_;
    }));

    it('provides the comments resource', function () {
        expect(IdentityResource.query).toBeDefined();
    });
});
