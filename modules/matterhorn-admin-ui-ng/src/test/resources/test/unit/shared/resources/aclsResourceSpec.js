describe('ACLs API Resource', function () {
    var AclsResource, $httpBackend, ResourceHelper;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _ResourceHelper_, _AclsResource_) {
        $httpBackend  = _$httpBackend_;
        AclsResource = _AclsResource_;
        ResourceHelper = _ResourceHelper_;
    }));

    describe('#query', function () {

        it('calls the acls service', function () {
            $httpBackend.expectGET('/admin-ng/acl/acls.json').respond(JSON.stringify(getJSONFixture('admin-ng/acl/acls.json')));
            AclsResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/acl/acls.json').respond(JSON.stringify(getJSONFixture('admin-ng/acl/acls.json')));
            var data = AclsResource.query();
            $httpBackend.flush();
            expect(data.rows[0].id).toBe(31151);
            expect(data.rows[0].name).toBe('•mock• Sample ACL');
        });
    });
});
