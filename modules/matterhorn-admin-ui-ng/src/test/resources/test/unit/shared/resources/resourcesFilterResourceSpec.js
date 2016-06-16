describe('Resource Filter API Resource', function () {
    var ResourcesFilterResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _ResourcesFilterResource_) {
        $httpBackend  = _$httpBackend_;
        ResourcesFilterResource = _ResourcesFilterResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, filtersResponse = {
                location: {
                    label: 'LOCATION',
                    type: 'select',
                    options: {
                        option1: 'OPTION1'
                    }
                }
            };
            $httpBackend.expectGET('/admin-ng/resources/events/filters.json')
                .respond(JSON.stringify(filtersResponse));
            result = ResourcesFilterResource.get({ resource: 'events' });
            $httpBackend.flush();
            expect(result.filters.location.options.option1).toEqual('OPTION1');
        });
    });
});
