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
            $httpBackend.whenGET('/admin-ng/events/40518/workflows/8695.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/workflows/8695.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/workflows/8695.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/workflows/8695.json')));
            EventWorkflowDetailsResource.get({ id0: '40518', id1: '8695' });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/admin-ng/event/40518/workflows/8695.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/workflows/8695.json')));
            var data = EventWorkflowDetailsResource.get({ id0: '40518', id1: '8695' });
            $httpBackend.flush();
            expect(data.duration).toEqual('472089');
        });
    });
});
