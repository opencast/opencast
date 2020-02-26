describe('User Roles API Resource', function () {
    var RolesResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _RolesResource_) {
        $httpBackend  = _$httpBackend_;
        RolesResource = _RolesResource_;
    }));

    it('provides the user roles resource', function () {
        expect(RolesResource.query).toBeDefined();
    });
});
