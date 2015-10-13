describe('Version API Resource', function () {
    var VersionResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _VersionResource_) {
        $httpBackend  = _$httpBackend_;
        VersionResource = _VersionResource_;
    }));

    it('provides the version resource', function () {
        expect(VersionResource.query).toBeDefined();
    });
});
