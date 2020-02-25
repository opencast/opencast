describe('Event Workflow Operations API Resource', function () {
    var $httpBackend, EventWorkflowOperationsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventWorkflowOperationsResource_) {
        $httpBackend  = _$httpBackend_;
        EventWorkflowOperationsResource = _EventWorkflowOperationsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations.json')));
        });

        it('queries the API', function () {
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations.json');
            EventWorkflowOperationsResource.get({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', id1: '1676' });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations.json');
            var data = EventWorkflowOperationsResource.get({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', id1: '1676' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.entries.length).toEqual(29);
        });
    });

    describe('#save', function () {
        it('sends an array of operations', function () {
            var metadataRequest = {
                entries: [{ id: 1302 }, { id: 1303 }]
            };
            $httpBackend.expectPOST('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations.json', metadataRequest.entries)
                .respond(200);
            EventWorkflowOperationsResource.save({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', id1: '1676' }, metadataRequest);
            $httpBackend.flush();
        });
    });
});
