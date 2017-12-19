describe('Opt Out API Resource', function () {
    var OptoutsResource, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _OptoutsResource_) {
        $httpBackend  = _$httpBackend_;
        OptoutsResource = _OptoutsResource_;
    }));

    describe('#save', function () {

        it('POSTs to the API', function () {
            $httpBackend.expectPOST('/admin-ng/event/optouts').respond(200, '{}');
            OptoutsResource.save({}, {
                resource: 'event',
                eventIds: [723, 912],
                optout:   true
            });
            $httpBackend.flush();
        });
    });
});
