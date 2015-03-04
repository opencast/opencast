describe('Event Workflows API Resource', function () {
    var EventWorkflowsResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventWorkflowsResource_) {
        $httpBackend  = _$httpBackend_;
        EventWorkflowsResource = _EventWorkflowsResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result,
                workflowsResponse = {
                    results: [{ id: 'title' }, { id: 'series' }]
                };

            $httpBackend.expectGET('/admin-ng/event/40518/workflows.json')
                .respond(JSON.stringify(workflowsResponse));

            result = EventWorkflowsResource.get({ id: 40518 });
            $httpBackend.flush();
            expect(result.entries.length).toBe(2);
        });
    });

    describe('#save', function () {
        it('sends an array of workflows', function () {
            var workflowsRequest = {
                entries: [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPOST('/admin-ng/event/40518/workflows', workflowsRequest.entries).respond(200);
            EventWorkflowsResource.save({ id: '40518' }, workflowsRequest);
            $httpBackend.flush();
        });
    });
});
