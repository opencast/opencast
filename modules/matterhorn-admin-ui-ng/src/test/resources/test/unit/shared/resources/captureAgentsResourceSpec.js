describe('Recordings API Resource', function () {
    var CaptureAgentsResource, $httpBackend;

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            formatDate:     function (val, date) { return date; },
            formatDateTime: function (val, date) { return date; },
            formatTime:     function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _CaptureAgentsResource_) {
        $httpBackend  = _$httpBackend_;
        CaptureAgentsResource = _CaptureAgentsResource_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#query', function () {

        it('calls the recordings service', function () {
            $httpBackend.expectGET('/admin-ng/capture-agents/agents.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
            CaptureAgentsResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/capture-agents/agents.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
            var data = CaptureAgentsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(3);
            expect(data.rows[0].id).toBe(data.rows[0].name);
            expect(data.rows[0].status).toBe('ok');
            expect(data.rows[0].name).toBe('•mock• agent4');
            expect(data.rows[0].updated).toBe('2014-05-26T15:37:02Z');
        });

        describe('without a room ID', function () {
            beforeEach(function () {
                var response = getJSONFixture('admin-ng/capture-agents/agents.json');
                delete response.results[0].roomId;
                $httpBackend.whenGET('/admin-ng/capture-agents/agents.json')
                    .respond(JSON.stringify(response));
            });

            it('uses the name as the ID', function () {
                var data = CaptureAgentsResource.query();
                $httpBackend.flush();
                expect(data.rows[0].id).toEqual('•mock• agent4');
            });
        });
    });
});
