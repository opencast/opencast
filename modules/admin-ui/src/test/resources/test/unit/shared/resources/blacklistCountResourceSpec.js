describe('Blacklist Count API Resource', function () {
    var BlacklistCountResource, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _BlacklistCountResource_) {
        $httpBackend  = _$httpBackend_;
        BlacklistCountResource = _BlacklistCountResource_;
    }));

    describe('#save', function () {

        it('calls the email template service', function () {
            $httpBackend.expectGET('/blacklist/blacklistCount').respond(200, '{}');
            BlacklistCountResource.save();
            $httpBackend.flush();
        });

        it('retrieves the current blacklist count', function () {
            $httpBackend.whenGET('/blacklist/blacklistCount')
                .respond(JSON.stringify({ eventsCount: 3, coursesCount: 2 }));
            var data = BlacklistCountResource.save({}, {
                type:          'person',
                blacklistedId: 5123
            });
            $httpBackend.flush();

            expect(data.eventsCount).toBe(3);
        });
    });
});
