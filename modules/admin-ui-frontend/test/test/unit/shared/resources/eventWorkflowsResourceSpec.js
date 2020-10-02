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

            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows.json')
                .respond(JSON.stringify(workflowsResponse));

            result = EventWorkflowsResource.get({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' });
            $httpBackend.flush();
            expect(result.entries.length).toBe(2);
        });
    });

    describe('#save', function () {
        it('sends an array of workflows', function () {
            var workflowsRequest = { id: 'title', configuration: {}};

            $httpBackend.expectPUT('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows').respond(200);
            EventWorkflowsResource.save({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' }, workflowsRequest);
            $httpBackend.flush();
        });
    });
});
