describe('Event Errors API Resource', function () {
    var EventErrorsResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventErrorsResource_) {
        $httpBackend  = _$httpBackend_;
        EventErrorsResource = _EventErrorsResource_;
    }));

    it('provides the resource', function () {
        $httpBackend.expectGET('/admin-ng/event/831/workflows/612/errors.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/workflows/8695/errors.json')));
        var data = EventErrorsResource.get({ id0: 831, id1: 612 });
        $httpBackend.flush();

        expect(data).toBeDefined();
    });
});
