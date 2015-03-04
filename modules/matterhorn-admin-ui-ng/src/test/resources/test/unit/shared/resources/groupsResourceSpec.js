describe('Groups API Resource', function () {
    var GroupsResource, $httpBackend;

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _GroupsResource_) {
        $httpBackend  = _$httpBackend_;
        GroupsResource = _GroupsResource_;
    }));

    describe('#query', function () {
        var sampleJSON = {
                count: 2,
                total: 2,
                limit: 0,
                offset: 0,
                results: [{
                    id: 'it_team',
                    name: 'IT Team',
                    description: 'The IT Team',
                    role: 'ROLE_GROUP_IT_TEAM'
                }, {
                    id: 'sales_team',
                    name: 'Sales Team',
                    description: 'The Sales Team',
                    role: 'ROLE_GROUP_SALES_TEAM'
                }]
        };

        it('calls the groups service', function () {
            $httpBackend.expectGET('/admin-ng/groups/groups.json').respond(JSON.stringify(sampleJSON));
            GroupsResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/groups/groups.json').respond(JSON.stringify(sampleJSON));
            var data = GroupsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(2);
            expect(data.rows[0].id).toBe(sampleJSON.results[0].id);
            expect(data.rows[0].name).toBe(sampleJSON.results[0].name);
            expect(data.rows[0].description).toBe(sampleJSON.results[0].description);
        });

        it('handles empty payload', function () {
            $httpBackend.whenGET('/admin-ng/groups/groups.json').respond('{}');
            var data = GroupsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(0);
        });

        it('handles payloads with one item', function () {
            $httpBackend.whenGET('/admin-ng/groups/groups.json').respond(JSON.stringify(
                {
                  count: 1,
                  limit: 0,
                  offset: 0,
                  results: [{
                      id: 'it_team',
                      name: 'IT Team',
                      description: 'The IT Team',
                      role: 'ROLE_GROUP_IT_TEAM',
                  }],
                  total: 1
                }
            ));
            var data = GroupsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(1);
        });
    });
});
