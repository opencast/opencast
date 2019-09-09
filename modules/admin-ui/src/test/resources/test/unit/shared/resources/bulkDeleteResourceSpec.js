describe('Bulk Delete API Resource', function () {
    var BulkDeleteResource, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _BulkDeleteResource_) {
        $httpBackend  = _$httpBackend_;
        BulkDeleteResource = _BulkDeleteResource_;
    }));

    describe('#delete', function () {

        it('POSTs to the API', function () {
            $httpBackend.expectPOST('/admin-ng/event/deleteEvents').respond(200, '{}');
            BulkDeleteResource.delete({}, {
                resource: 'event',
                endpoint: 'deleteEvents',
                eventIds: [723, 912]
            });
            $httpBackend.flush();
        });
    });
});
