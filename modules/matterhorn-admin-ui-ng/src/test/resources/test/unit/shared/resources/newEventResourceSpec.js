describe('New Event API Resource', function () {
    var NewEventResource, $httpBackend, JsHelper,
        singleTestData, multiTestData, uploadTestData,
        expectedSingle, expectedSourceSingle, expectedSourceMultiple,
        startDate, endDate, date;

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/test/unit/fixtures';
        singleTestData = getJSONFixture('newEventSingleFixture.json');
        multiTestData = getJSONFixture('newEventMultipleFixture.json');
        uploadTestData = getJSONFixture('newEventUploadFixture.json');
    });

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _NewEventResource_, _JsHelper_) {
        NewEventResource = _NewEventResource_;
        $httpBackend = _$httpBackend_;
        JsHelper = _JsHelper_;

        date = new Date ('2014', '7', '17', '10', '0');
        startDate = JsHelper.toZuluTimeString({
            date: '2014-07-17',
            hour:  '10',
            minute: '0'
        });   

        endDate = JsHelper.toZuluTimeString({
            date: '2014-07-24',
            hour:  '10',
            minute: '0'
        }, {
            hour: '1',
            minute: '45'
        });

        expectedSourceMultiple = {
            'type': 'SCHEDULE_MULTIPLE',
            'metadata': {
                'start'   : startDate,
                'end'     : endDate,
                'duration': '6300000',
                'rrule'   : 'FREQ=WEEKLY;BYDAY=MO,TU,WE;BYHOUR=' + date.getUTCHours() + ';BYMINUTE=0',
                'device'  : '•mock• agent3',
                'inputs'  : 'TRANSLATION.PATH.VIDEO'
            }
        };

        date = new Date ('2014-07-16T06:06:00Z');
        startDate = JsHelper.toZuluTimeString({
            date: '2014-07-16',
            hour:  '02',
            minute: '06'
        });   

        endDate = JsHelper.toZuluTimeString({
            date: '2014-07-16',
            hour:  '02',
            minute: '06'
        }, {
            hour   : '02',
            minute : '03'
        });


        expectedSourceSingle = {
            'type': 'SCHEDULE_SINGLE',
            'metadata': {
                'start' : startDate,
                'end'   : endDate,
                'device': '•mock• agent3',
                'inputs': 'TRANSLATION.PATH.VIDEO'
            }
        };

        expectedSingle = {
            'metadata': {
                'presenters': {
                    '$$hashKey': '026',
                    'collection': 'users',
                    'id': 'presenters',
                    'label': 'EVENTS.EVENTS.DETAILS.METADATA.PRESENTER',
                    'presentableValue': [
                        'chuck.norris'
                    ],
                    'readOnly': false,
                    'required': 'true',
                    'type': 'text',
                    'value': [
                        'chuck.norris'
                    ]
                },
                'subject': {
                    '$$hashKey': '029',
                    'id': 'subject',
                    'label': 'EVENTS.EVENTS.DETAILS.METADATA.SUBJECT',
                    'presentableValue': [
                        'grunz'
                    ],
                    'readOnly': false,
                    'required': 'true',
                    'type': 'text',
                    'value': [
                        'grunz'
                    ]
                },
                'title': {
                    '$$hashKey': '025',
                    'id': 'title',
                    'label': 'EVENTS.EVENTS.DETAILS.METADATA.TITLE',
                    'presentableValue': 'test',
                    'readOnly': false,
                    'required': 'true',
                    'type': 'text',
                    'value': 'test'
                }
            },
            'processing': {
                'workflow': 'default',
                'configuration': { }
            },
            'access': {
                'id': '1601'
            }
        };
    }));


    describe('assembles the metadata for', function () {
        var args;

        beforeEach(function () {
            $httpBackend.expectPOST('/admin-ng/event/new').respond(200);

            spyOn(FormData.prototype, 'append').and.callThrough();

            NewEventResource.save(singleTestData);
            $httpBackend.flush();
            args = JSON.parse(FormData.prototype.append.calls.allArgs()[0][1]);
        });

        it('#source SCHEDULE_SINGLE', function () {
            expect(args.source).toEqual(expectedSourceSingle);
        });

        it('#metadata', function () {
            expect(args.metadata.metadata).toEqual(expectedSingle.metadata);
        });

        it('#processing', function () {
            expect(args.processing).toEqual(expectedSingle.processing);
        });

        it('#access', function () {
            expect(args.access).toEqual(expectedSingle.access);
        });
    });

    it('assembles the metadata for SCHEDULE_MULTIPLE', function () {
        var args;
        $httpBackend.expectPOST('/admin-ng/event/new').respond(200);

        spyOn(FormData.prototype, 'append').and.callThrough();
        NewEventResource.save(multiTestData);
        $httpBackend.flush();
        args = JSON.parse(FormData.prototype.append.calls.allArgs()[0][1]);

        expect(args.source).toEqual(expectedSourceMultiple);
    });

    describe('UPLOAD', function () {
        var args;
        beforeEach(function () {
            $httpBackend.expectPOST('/admin-ng/event/new').respond(200);
            spyOn(FormData.prototype, 'append').and.callThrough();
        });

        it('assembles the metadata for audio only', function () {
            NewEventResource.save(uploadTestData);
            $httpBackend.flush();
            args = JSON.parse(FormData.prototype.append.calls.allArgs()[0][1]);

            expect(args.source).toEqual({ type: 'UPLOAD' });
        });

        it('assembles the metadata for segmentable video', function () {
            delete uploadTestData.source.upload.audioOnly;
            uploadTestData.source.upload.segmentable = {};

            NewEventResource.save(uploadTestData);
            $httpBackend.flush();
            args = JSON.parse(FormData.prototype.append.calls.allArgs()[0][1]);

            expect(args.source).toEqual({ type: 'UPLOAD' });
        });

        it('assembles the metadata for non-segmentable video', function () {
            delete uploadTestData.source.upload.segmentable;
            uploadTestData.source.upload.nonSegmentable = {};

            NewEventResource.save(uploadTestData);
            $httpBackend.flush();
            args = JSON.parse(FormData.prototype.append.calls.allArgs()[0][1]);

            expect(args.source).toEqual({ type: 'UPLOAD' });
        });

        it('handles empty video format gracefully', function () {
            delete uploadTestData.source.upload.nonSegmentable;

            NewEventResource.save(uploadTestData);
            $httpBackend.flush();
            args = JSON.parse(FormData.prototype.append.calls.allArgs()[0][1]);

            expect(args.source).toEqual({ type: 'UPLOAD' });
        });
    });

    it('handles empty requests gracefully', function () {
        $httpBackend.expectPOST('/admin-ng/event/new').respond(200);
        NewEventResource.save();
        $httpBackend.flush();
    });
});
