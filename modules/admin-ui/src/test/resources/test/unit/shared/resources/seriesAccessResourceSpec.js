describe('Series Access API Resource', function () {
    var SeriesAccessResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));


    beforeEach(inject(function (_$httpBackend_, _SeriesAccessResource_) {
        $httpBackend  = _$httpBackend_;
        SeriesAccessResource = _SeriesAccessResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, accessResponse = {
                series_access: {
                    current_acl: 1601,
                    privileges: {
                        ROLE_ADMIN: {
                            read: true,
                            write: false
                        },
                        ROLE_USER: {
                            read: true,
                            write: true
                        }
                    },
                    transitions: []
                },
                system_acls: [{
                    id: 1601,
                    name: 'Test'
                }, {
                    id: 1851,
                    name: '2nd ACL'
                }]
            };

            $httpBackend.expectGET('/admin-ng/series/20518/access.json').respond(JSON.stringify(accessResponse));
            result = SeriesAccessResource.get({ id: 20518 });
            $httpBackend.flush();
            expect(result.series_access.current_acl).toBe(1601);
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
            };
            $httpBackend.expectPOST('/admin-ng/series/40518/access', function (data) {
                var expected = {
                    acl: angular.toJson({acl: accessRequest.acl}),
                    override: 'true'
                };
                expect(angular.fromJson($.deparam(data))).toEqual(expected);
                return true;
            }).respond(200);
            SeriesAccessResource.save({ id: '40518' }, accessRequest);
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.expectPOST('/admin-ng/series/access').respond(200);
            SeriesAccessResource.save();
            $httpBackend.flush();
        });
    });
});
