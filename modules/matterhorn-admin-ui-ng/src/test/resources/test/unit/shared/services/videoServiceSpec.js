describe('VideoService', function () {
    var VideoService;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));

    beforeEach(inject(function (_VideoService_) {
        VideoService = _VideoService_;
    }));

    it('instantiates', function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        expect(VideoService.getCurrentSegment).toBeDefined();
    });

    describe('#getCurrentSegment', function () {
        beforeEach(function () {
            this.player = {};
            this.video = angular.copy(getJSONFixture('admin-ng/tools/40518/editor.json'));
        });

        describe('with the cursor at the start', function () {
            beforeEach(function () {
                this.player.adapter = {
                    getCurrentTime: function () { return 0; }
                };
            });

            it('returns the first segment of the track', function () {
                var segment = VideoService.getCurrentSegment(this.player, this.video);
                expect(segment).toEqual({ start: 0, end: 17003 });
            });
        });

        describe('with the cursor at the end', function () {
            beforeEach(function () {
                this.player.adapter = {
                    getCurrentTime: function () { return 46.230; }
                };
            });

            it('returns the last segment of the track', function () {
                var segment = VideoService.getCurrentSegment(this.player, this.video);
                expect(segment).toEqual({ start: 28009, end: 52125 });
            });
        });

        describe('with the cursor in between', function () {
            beforeEach(function () {
                this.player.adapter = {
                    getCurrentTime: function () { return 19.817; }
                };
            });

            it('returns the current segment', function () {
                var segment = VideoService.getCurrentSegment(this.player, this.video);
                expect(segment).toEqual({ start: 17003, end: 28009 });
            });
        });
    });
});
