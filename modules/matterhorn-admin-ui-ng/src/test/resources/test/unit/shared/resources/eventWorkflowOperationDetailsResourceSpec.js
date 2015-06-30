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
            $httpBackend.whenGET('/admin-ng/event/40518/workflows/8695/operations/1')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/workflows/8695/operations/1303.json')));
        });

        it('queries the API', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/workflows/8695/operations/1').respond(getJSONFixture('admin-ng/event/40518/workflows/8695/operations/1303.json'));
            EventWorkflowOperationDetailsResource.get({ id0: '40518', id1: '8695', id2: 1 });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/workflows/8695/operations/1').respond(getJSONFixture('admin-ng/event/40518/workflows/8695/operations/1303.json'));
            var data = EventWorkflowOperationDetailsResource.get({ id0: '40518', id1: '8695', id2: 1 });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.id).toEqual(1303);
        });
    });
});
