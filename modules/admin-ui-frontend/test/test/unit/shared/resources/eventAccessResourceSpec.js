describe('Event Access API Resource', function () {
    var $httpBackend, EventAccessResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventAccessResource_) {
        $httpBackend  = _$httpBackend_;
        EventAccessResource = _EventAccessResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/access.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/access.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/access.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/access.json')));
            EventAccessResource.get({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a'});
        });

        it('returns the parsed JSON', function () {
            $httpBackend.whenGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/access.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/access.json')));
            var data = EventAccessResource.get({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.system_acls.length).toBe(3);
        });
    });

    describe('#save', function () {
        it('sends an array of metadata', function () {
            var accessRequest = {
                acl: {'ace': [
                        {'action':'read','allow':true,'role':'ROLE_ADMIN'},
                        {'action':'write','allow':true,'role':'ROLE_ADMIN'},
                        {'action':'read','allow':true,'role':'ROLE_ANONYMOUS'},
                        {'action':'write','allow':true,'role':'ROLE_ANONYMOUS'}
                ]},
                override: true
            },
            expectedTransformation = {
                acl: angular.toJson({acl: accessRequest.acl}),
                override: 'true'
            };
            $httpBackend.expectPOST('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/access', function (data) {
                expect($.deparam(data)).toEqual(expectedTransformation);
                return true;
            }).respond(200);
            EventAccessResource.save({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a' }, accessRequest);
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.expectPOST('/admin-ng/event/access').respond(200);
            EventAccessResource.save();
            $httpBackend.flush();
        });
    });
});
