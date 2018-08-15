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
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/workflows/1676/errors.json')));
        var data = EventErrorsResource.get({ id0: 831, id1: 612 });
        $httpBackend.flush();

        expect(data).toBeDefined();
    });
});
