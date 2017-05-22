describe('EventWorkflowActionResource Test', function () {
    var EventWorkflowActionResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventWorkflowActionResource_) {
        $httpBackend  = _$httpBackend_;
        EventWorkflowActionResource = _EventWorkflowActionResource_;
    }));

    describe('#save', function () {
        it('PUTs to the API', function () {
            $httpBackend.expectPUT('/admin-ng/event/1234/workflow/action/RETRY').respond(200, '{}');
            EventWorkflowActionResource.save({}, {
                id: '1234',
                action: 'RETRY'
            });
            $httpBackend.flush();
        });
    });
});
