describe('Jobs API Resource', function () {
    var JobsResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services.language'));
    beforeEach(module('pascalprecht.translate'));
    beforeEach(module('ngResource'));
    beforeEach(module('LocalStorageModule'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDateTimeRaw:   function (val, date) { return date; },
            formatDateTime:      function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _JobsResource_) {
        $httpBackend  = _$httpBackend_;
        JobsResource = _JobsResource_;
    }));

    describe('#query', function () {
        var sampleJSON = {
            count: 2,
            limit: 2,
            offset: 0,
            results: [{
                operation: 'START_OPERATION',
                status: 'SUCCEEDED',
                submitted: 'Mon Mar 31 08:32:39 CEST 2014',
                title: 'New job',
                workflow: 'Encode, Analyze, and Distribute'
            }, {
                operation: 'START_OPERATION',
                status: 'SUCCEEDED',
                submitted: 'Mon Mar 31 08:33:40 CEST 2014',
                title: 'Job with serie',
                workflow: 'Encode, Analyze, and Distribute'
            }],
            total: 2
        };

        it('calls the jobs service', function () {
            $httpBackend.expectGET('/admin-ng/job/jobs.json').respond(JSON.stringify(sampleJSON));
            JobsResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/job/jobs.json').respond(JSON.stringify(sampleJSON));
            var data = JobsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(2);
            expect(data.rows[0].operation).toBe(sampleJSON.results[0].operation);
            expect(data.rows[0].name).toBe(sampleJSON.results[0].name);
            expect(data.rows[0].status).toBe(sampleJSON.results[0].status);
        });
    });
});
