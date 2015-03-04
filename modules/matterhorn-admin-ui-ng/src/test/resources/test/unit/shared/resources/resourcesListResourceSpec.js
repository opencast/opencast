describe('Resource List API Resource', function () {
    var ResourcesListResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _ResourcesListResource_) {
        $httpBackend  = _$httpBackend_;
        ResourcesListResource = _ResourcesListResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, listResponse = { item1: 'Item 1', item2: 'Item 2' };
            $httpBackend.expectGET('/admin-ng/resources/users.json').respond(JSON.stringify(listResponse));
            result = ResourcesListResource.get({ resource: 'users' });
            $httpBackend.flush();
            expect(result.item1).toEqual('Item 1');
        });
    });
});
