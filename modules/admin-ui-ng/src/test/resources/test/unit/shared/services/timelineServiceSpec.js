xdescribe('Timeline', function () {
    var $httpBackend, Timeline, PlayerAdapter, PlayerAdapterFactoryHTML5;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));

    beforeEach(inject(function (_$httpBackend_, _Timeline_, _PlayerAdapter_, _PlayerAdapterFactoryHTML5_) {
        $httpBackend = _$httpBackend_;

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').
            respond(JSON.stringify(getJSONFixture('info/me.json')));

        Timeline = _Timeline_;
        PlayerAdapter = _PlayerAdapter_;
        PlayerAdapterFactoryHTML5 = _PlayerAdapterFactoryHTML5_;
    }));

    beforeEach(function () {
        var start  = new Date(0),
            middle = new Date(0),
            end    = new Date(0);
        middle.setSeconds(14);
        middle.setMilliseconds(413);
        end.setSeconds(20);
        end.setMilliseconds(230);

        Timeline.timeline = {
            itemsData: new vis.DataSet([{
                id:        1,
                content:   'Sample Audio 1',
                group:     'waveform',
                start:     start,
                end:       middle,
            }, {
                id:        2,
                content:   'Sample Audio 2',
                group:     'waveform',
                start:     middle,
                end:       end,
            }, {
                id:        3,
                content:   'Sample Segment 1',
                group:     'video track',
                start:     start,
                end:       middle,
                className: 'foo'
            }, {
                id:        4,
                content:   'Sample Segment 2',
                group:     'video track',
                start:     middle,
                end:       end,
                className: 'foo'
            }])
        };
    });

    it('instantiates', function () {
        expect(Timeline.create).toBeDefined();
    });

    describe('#getCurrentSegments', function () {

        describe('with the cursor at the start', function () {
            beforeEach(function () {
                Timeline.player = {
                    getCurrentTime: function () { return 0; }
                };
            });

            it('returns the first segments of each track', function () {
                expect(Timeline.getCurrentSegments()).
                    toContain(Timeline.timeline.itemsData.get(1));
                expect(Timeline.getCurrentSegments()).
                    toContain(Timeline.timeline.itemsData.get(3));
            });
        });

        describe('with the cursor at the end', function () {
            beforeEach(function () {
                Timeline.player = {
                    getCurrentTime: function () { return 20.230; }
                };
            });

            it('returns the last segments of each track', function () {
                expect(Timeline.getCurrentSegments()).
                    toContain(Timeline.timeline.itemsData.get(2));
                expect(Timeline.getCurrentSegments()).
                    toContain(Timeline.timeline.itemsData.get(4));
            });
        });

        describe('with the cursor in between', function () {
            beforeEach(function () {
                Timeline.player = {
                    getCurrentTime: function () { return 12.817; }
                };
            });

            it('returns the current segments', function () {
                expect(Timeline.getCurrentSegments()).
                    toContain(Timeline.timeline.itemsData.get(1));
                expect(Timeline.getCurrentSegments()).
                    toContain(Timeline.timeline.itemsData.get(3));
            });
        });
    });

    describe('#cut', function () {

        beforeEach(function () {
            Timeline.player = {
                getCurrentTime: function () { return 16.123; }
            };
        });

        it('creates a subset for each segment at the current cursor', function () {
            expect(Timeline.timeline.itemsData.get().length).toBe(4);
            Timeline.cut();
            expect(Timeline.timeline.itemsData.get().length).toBe(6);
        });

        it('does not create overlaps', function () {
            Timeline.cut();

            expect(Timeline.timeline.itemsData.get(5).start.getSeconds()).toEqual(16);
            expect(Timeline.timeline.itemsData.get(5).start.getMilliseconds()).toEqual(123);
            expect(Timeline.timeline.itemsData.get(6).start.getSeconds()).toEqual(16);
            expect(Timeline.timeline.itemsData.get(6).start.getMilliseconds()).toEqual(123);

            expect(Timeline.timeline.itemsData.get(2).end.getSeconds()).toEqual(16);
            expect(Timeline.timeline.itemsData.get(2).end.getMilliseconds()).toEqual(123);
            expect(Timeline.timeline.itemsData.get(4).end.getSeconds()).toEqual(16);
            expect(Timeline.timeline.itemsData.get(4).end.getMilliseconds()).toEqual(123);
        });
    });
});
