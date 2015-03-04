describe('Services API Resource', function () {
    var ServicesResource, $httpBackend;

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _ServicesResource_) {
        $httpBackend  = _$httpBackend_;
        ServicesResource = _ServicesResource_;
    }));

    describe('#query', function () {
        var sampleJSON = {
            'limit': 'Some(0)',
            'total': '39',
            'results': [
                {
                    'host': 'http://mh-allinone.localdomain',
                    'queued': '0',
                    'status': 'NORMAL',
                    'name': 'org.opencastproject.userdirectory.roles',
                    'meanQueueTime': '0',
                    'running': '0',
                    'meanRunTime': '0',
                    'completed': '0'
                },
                {
                    'host': 'http://mh-allinone.localdomain',
                    'queued': '0',
                    'status': 'OFFLINE',
                    'name': 'org.opencastproject.userdirectory.users',
                    'meanQueueTime': '0',
                    'running': '0',
                    'meanRunTime': '0',
                    'completed': '0'
                }
            ],
            'offset': 'Some(0)'
        };
        it('calls the services service', function () {
            $httpBackend.expectGET('/admin-ng/services/services.json').respond(JSON.stringify(sampleJSON));
            ServicesResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/services/services.json').respond(JSON.stringify(sampleJSON));
            var data = ServicesResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(2);
        });
    });
});
