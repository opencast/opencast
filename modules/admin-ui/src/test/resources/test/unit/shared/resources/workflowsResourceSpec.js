describe('Workflows API Resource', function () {
    var $httpBackend, WorkflowsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _WorkflowsResource_) {
        $httpBackend  = _$httpBackend_;
        WorkflowsResource = _WorkflowsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        });

        it('queries the workflow API', function () {
            $httpBackend.expectGET('/workflow/definitions.json')
                .respond(JSON.stringify(getJSONFixture('workflow/definitions.json')));

            WorkflowsResource.get();
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.whenGET('/workflow/definitions.json')
                .respond(JSON.stringify(getJSONFixture('workflow/definitions.json')));
            var data = WorkflowsResource.get();
            $httpBackend.flush();

            expect(data).toBeDefined();
            expect(data.length).toBe(14);
        });

        it('handles empty results gracefully', function () {
            $httpBackend.whenGET('/workflow/definitions.json')
                .respond(JSON.stringify({}));
            var data = WorkflowsResource.get();
            $httpBackend.flush();

            expect(data).toBeDefined();
            expect(data.length).toBe(0);
        });
    });
});
