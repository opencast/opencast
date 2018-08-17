describe('Event Error Details API Resource', function () {
    var EventErrorDetailsResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventErrorDetailsResource_) {
        $httpBackend  = _$httpBackend_;
        EventErrorDetailsResource = _EventErrorDetailsResource_;
    }));

    it('provides the resource', function () {
        $httpBackend.expectGET('/admin-ng/event/831/workflows/612/errors/9371.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/workflows/8695/errors/9371.json')));
        var data = EventErrorDetailsResource.get({ id0: 831, id1: 612, id2: 9371 });
        $httpBackend.flush();

        expect(data).toBeDefined();
        expect(data.id).toEqual(9371);
    });
});
