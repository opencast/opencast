describe('Service API Resource', function () {
    var $httpBackend, ServiceResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _ServiceResource_) {
        $httpBackend  = _$httpBackend_;
        ServiceResource = _ServiceResource_;
    }));

    describe('#setMaintenanceMode', function () {

        it('queries the services API', function () {
            $httpBackend.expectPOST('/services/maintenance').respond(200);
            ServiceResource.setMaintenanceMode({
                host: 'service1.mh.ch',
                maintenance: true
            });
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.whenPOST('/services/maintenance').respond(200);
            ServiceResource.setMaintenanceMode();
            $httpBackend.flush();
        });
    });

    describe('#sanitize', function () {
        beforeEach(function () {
        });

        it('queries the services API', function () {
            $httpBackend.expectPOST('/services/sanitize').respond(200);
            ServiceResource.sanitize({
                host: 'service1.mh.ch',
                serviceType: 'ENCODER'
            });
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.whenPOST('/services/sanitize').respond(200);
            ServiceResource.sanitize();
            $httpBackend.flush();
        });
    });
});
