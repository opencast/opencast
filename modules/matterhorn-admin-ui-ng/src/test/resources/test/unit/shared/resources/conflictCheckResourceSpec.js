describe('Conflict check API Resource', function () {
    var $httpBackend, ConflictCheckResource, singleTestData, multiTestData, conflictResponse, JsHelper,
        startDate, endDate;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _ConflictCheckResource_, _JsHelper_) {
        $httpBackend  = _$httpBackend_;
        ConflictCheckResource = _ConflictCheckResource_;
        JsHelper = _JsHelper_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/test/unit/fixtures';
        singleTestData = getJSONFixture('conflictCheckSingle.json');
        multiTestData = getJSONFixture('conflictCheckMultiple.json');
        conflictResponse = getJSONFixture('conflictResponse.json');
    });

    describe('when schedule multiple is active', function () {
        var expectedOutput;
        
        beforeEach(function () {
            var date = new Date ('2014', '7', '23', '17', '0'),
                endFirstLecture;
            startDate = JsHelper.toZuluTimeString({
                date   : '2014-07-08',
                hour   : 17,
                minute : 0
            });


            endFirstLecture = JsHelper.toZuluTimeString({
                date   : '2014-07-08',
                hour   : 17,
                minute : 0
            }, {
                hour: 1,
                minute: 0

            });            

            endDate = JsHelper.toZuluTimeString({
                date   : '2014-07-23',
                hour   : 17,
                minute : 0
            }, {
                hour: 1,
                minute: 0

            });

            expectedOutput = {
                'start'    : startDate,
                'end'      : endDate,
                'duration' : '3600000',
                'rrule'    : 'FREQ=WEEKLY;BYDAY=MO,WE,TH;BYHOUR=' + date.getUTCHours() + ';BYMINUTE=0',
                'device'   : '•mock• agent4'
            };
        });

        it('basic wiring works', function () {
            $httpBackend.expectPOST('/admin-ng/event/new/conflicts').respond(204, '');
            ConflictCheckResource.check(multiTestData);
            $httpBackend.flush();
        });

        it('transformRequest works as expected', function () {
            $httpBackend.expectPOST('/admin-ng/event/new/conflicts', function (request) {
                var decoded = angular.fromJson($.deparam(request).metadata);
                expect(decoded).toEqual(expectedOutput);
                return true;
            }).respond(204, '');
            ConflictCheckResource.check(multiTestData);
            $httpBackend.flush();
        });
    });

    describe('when schedule single is active', function () {

        var expectedOutput;

        beforeEach(function () {
            startDate = JsHelper.toZuluTimeString({
                date   : '2014-07-16',
                hour   : '8',
                minute : '0'
            });

            endDate = JsHelper.toZuluTimeString({
                date   : '2014-07-16',
                hour   : '8',
                minute : '0'
            }, {
                hour: 4,
                minute: 4
            });
            
            expectedOutput = {
                'start'    : startDate,
                'end'      : endDate,
                'device'   : 'entwine-captureagent',
                'duration' : '14640000'
            };
        });


        it('transforms the request correctly', function () {
            $httpBackend.expectPOST('/admin-ng/event/new/conflicts', function (request) {
                var decoded = angular.fromJson($.deparam(request).metadata);
                expect(decoded).toEqual(expectedOutput);
                return true;
            }).respond(204, '');
            ConflictCheckResource.check(singleTestData.SCHEDULE_SINGLE);
            $httpBackend.flush();
        });
    });
});
