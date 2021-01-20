describe('Event Workflow Details API Resource', function () {
    var $httpBackend, EventWorkflowDetailsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventWorkflowDetailsResource_) {
        $httpBackend  = _$httpBackend_;
        EventWorkflowDetailsResource = _EventWorkflowDetailsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/events/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676.json')));
            EventWorkflowDetailsResource.get({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', id1: '1676' });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676.json')));
            var data = EventWorkflowDetailsResource.get({ id0: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', id1: '1676' });
            $httpBackend.flush();
            expect(data.duration).toEqual(179169);
        });
    });
});
