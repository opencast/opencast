describe('Source Step in New Event Wizard', function () {
    var NewEventSource, $httpBackend, Notifications, captureAgents;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate:     function (val, date) { return date; },
            formatDateTime: function (val, date) { return date; },
            formatTime:     function (val, date) { return date; },
            toLocalTime:    function () { return ''; }
        });
    }));

    beforeEach(inject(function (_NewEventSource_, _$httpBackend_, _Notifications_) {
        NewEventSource = _NewEventSource_;
        $httpBackend = _$httpBackend_;
        Notifications = _Notifications_;
        spyOn(Notifications, 'add').and.callThrough();
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        captureAgents = getJSONFixture('admin-ng/capture-agents/agents.json');
    });

    describe('#isValid', function () {

        it('is defined', function () {
            var isValid = NewEventSource.isValid();
            expect(isValid).toBeDefined();
        });

        describe('with the source toggle set to later', function () {
            beforeEach(function () {
                NewEventSource.ud.type = 'later';
            });

            it('validates', function () {
                expect(NewEventSource.isValid()).toBe(true);
            });
        });

        describe('with the source toggle set to single', function () {
            var ud, 
                insertValidValues = function () {
                    ud.SCHEDULE_SINGLE.start.date      = new Date();
                    ud.SCHEDULE_SINGLE.start.hour      = '10';
                    ud.SCHEDULE_SINGLE.start.minute    = '02';
                    ud.SCHEDULE_SINGLE.duration.hour   = '01';
                    ud.SCHEDULE_SINGLE.duration.minute = '33';
                    ud.SCHEDULE_SINGLE.device =
                    {
                        'name': '•mock• agent4',
                        'Status': 'ok',
                        'Update': '2014-05-26T15:37:02Z',
                        'blacklist': '',
                        'inputs': []
                };
                ud.SCHEDULE_SINGLE.device.inputMethods = {};
                ud.SCHEDULE_SINGLE.device.inputMethods.video = true;
            };
            beforeEach(function () {
                ud = NewEventSource.ud;
                ud.type = 'SCHEDULE_SINGLE';
                ud.SCHEDULE_SINGLE = {
                    start: {},
                    duration: {},
                    inputMethods: {},
                    device: {
                        name: 'test device',
                        inputMethod: {}
                    }
                };
            });

            it('validates', function () {
                expect(NewEventSource.isValid()).toBeFalsy();
                insertValidValues();
                expect(NewEventSource.isValid()).toBeTruthy();
            });
        });

        describe('with the source toggle set to multiple', function () {
            var ud;
            beforeEach(function () {
                ud = NewEventSource.ud;
                ud.type = 'SCHEDULE_MULTIPLE';
                ud.SCHEDULE_MULTIPLE = {};
                ud.SCHEDULE_MULTIPLE.start = {};
                ud.SCHEDULE_MULTIPLE.end = {};
                ud.SCHEDULE_MULTIPLE.duration = {};
            });
            
            it('validates repetitions', function () {
                ud.SCHEDULE_MULTIPLE.start.date = new Date();
                ud.SCHEDULE_MULTIPLE.start.hour = '10';
                ud.SCHEDULE_MULTIPLE.start.minute = '09';
                ud.SCHEDULE_MULTIPLE.duration.hour = '10';
                ud.SCHEDULE_MULTIPLE.duration.minute = '09';
                ud.SCHEDULE_MULTIPLE.repetitionOption = 'daily';
                ud.SCHEDULE_MULTIPLE.device = {};
                ud.SCHEDULE_MULTIPLE.device.name  = 'test device';
                ud.SCHEDULE_MULTIPLE.device.inputMethods = {};
                ud.SCHEDULE_MULTIPLE.device.inputMethods.video = true;
                ud.SCHEDULE_MULTIPLE.weekdays = {MO: true};
                expect(NewEventSource.isValid()).toBeTruthy();
            });

            it('# the state is not ready to poll for conflicts without necessary user data', function () {
                expect(NewEventSource.readyToPollConflicts()).toBeFalsy();
            });

            it('# the state is ready to poll for conflicts with necessary user data', function () {
                expect(NewEventSource.readyToPollConflicts()).toBeFalsy();
                ud.SCHEDULE_MULTIPLE.start.date = '2014-07-01';
                expect(NewEventSource.readyToPollConflicts()).toBeFalsy();
                ud.SCHEDULE_MULTIPLE.end = '2014-08-01';
                expect(NewEventSource.readyToPollConflicts()).toBeFalsy();
                ud.SCHEDULE_MULTIPLE.device = {
                    id: 'an id, no matter which'
                };
                expect(NewEventSource.readyToPollConflicts()).toBeFalsy();
                ud.SCHEDULE_MULTIPLE.duration = {
                    hour: '01',
                    minute: '00'
                };
                expect(NewEventSource.readyToPollConflicts()).toBeFalsy();
                ud.SCHEDULE_MULTIPLE.weekdays = {MO: true};
                expect(NewEventSource.readyToPollConflicts()).toBeTruthy();
            });
        });
    });

    describe('conflict checking behaviour', function () {
        var conflictResponse, singleTestData;

        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/test/unit/fixtures';
            conflictResponse = getJSONFixture('conflictResponse.json');
            singleTestData = getJSONFixture('conflictCheckSingle.json');
            $httpBackend.expectPOST('/admin-ng/event/new/conflicts').respond(409, conflictResponse);
            $httpBackend.whenGET('/admin-ng/capture-agents/agents.json?inputs=true').respond(JSON.stringify(captureAgents));
            NewEventSource.ud = singleTestData;
            NewEventSource.checkConflicts();
            $httpBackend.flush();
        });

        it('notifies the user when a conflict is detected', function () {
            expect(Notifications.add.calls.count()).toBe(1);
        });

        it('becomes invalid if a conflict is detected', function () {
            expect(NewEventSource.isValid()).toBeFalsy();
        });

        it('pushes the conflict into the conflict array', function () {
            expect(NewEventSource.conflicts.length).toBe(1);
        });

        it('becomes valid again if the conflict is removed', function () {
            $httpBackend.expectPOST('/admin-ng/event/new/conflicts').respond(203);
            NewEventSource.checkConflicts();
            $httpBackend.flush();
            expect(NewEventSource.conflicts.length).toBe(0);
            expect(NewEventSource.isValid()).toBeTruthy();
        });
    });
});
