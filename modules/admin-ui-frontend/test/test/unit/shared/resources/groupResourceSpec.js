describe('Group API Resource', function () {
    var $httpBackend, GroupResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _GroupResource_) {
        $httpBackend  = _$httpBackend_;
        GroupResource = _GroupResource_;
    }));

    describe('#get', function () {
        var sampleJSON;
        beforeEach(function () {
            sampleJSON = {
                    id: 'it_team',
                    name: 'IT Team',
                    description: 'The IT Team',
                    role: 'ROLE_GROUP_IT_TEAM',
                    members: {
                        member: 'admin'
                    },
                    roles: {
                        role: [{
                            name: 'ROLE_ADMIN'
                        }]
                    }
            };
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/admin-ng/groups/it_team').respond(JSON.stringify(sampleJSON));
            GroupResource.get({ id: 'it_team' });
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.whenGET('/admin-ng/groups/it_team').respond(JSON.stringify(sampleJSON));
            var data = GroupResource.get({ id: 'it_team' });
            $httpBackend.flush();
            expect(data.name).toEqual('IT Team');
        });
    });
});
