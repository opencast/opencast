describe('User Roles API Resource', function () {
    var UserRolesResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _UserRolesResource_) {
        $httpBackend  = _$httpBackend_;
        UserRolesResource = _UserRolesResource_;
    }));

    it('provides the user roles resource', function () {
        expect(UserRolesResource.query).toBeDefined();
    });
});
