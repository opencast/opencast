describe('Event Workflow Operation API Resource', function () {
    var $httpBackend, EventWorkflowOperationDetailsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventWorkflowOperationDetailsResource_) {
        $httpBackend  = _$httpBackend_;
        EventWorkflowOperationDetailsResource = _EventWorkflowOperationDetailsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations/1')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations/516.json')));
        });

        it('queries the API', function () {
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations/1').respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations/516.json')));
            EventWorkflowOperationDetailsResource.get({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', id1: '1676', id2: 1 });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations/1').respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/operations/516.json')));
            var data = EventWorkflowOperationDetailsResource.get({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', id1: '1676', id2: 1 });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.id).toEqual(516);
        });
    });
});
