describe('adminNg.directives.timelineDirective', function () {
    var $compile, $rootScope, $document, element, spy;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/timeline.html'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
        });
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_, _$document_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $document = _$document_;
    }));

    beforeEach(function () {
        $rootScope.video = {};
        $rootScope.player = {
            adapter: {
                addListener: function (event, callback) {
                    spy = callback;
                },
                getCurrentTime: function () {
                    return 52;
                },
                setCurrentTime: jasmine.createSpy()
            }
        };
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $rootScope.video = angular.copy(getJSONFixture('admin-ng/tools/40518/editor.json'));
        element = $compile('<div data-admin-ng-timeline="" data-video="video" data-player="player"/></div>')($rootScope);
        $rootScope.$digest();
    });

    it('renders available tracks', function () {
        expect(element).toContainElement('.tracks');
        expect(element.find('.timeline-track').length).toBe(2);
    });

    it('millisecond display', function(){

        var f = element.isolateScope().formatMilliseconds,
            g = element.isolateScope().displayZoomLevel;

        // formatMilliseconds
        expect(f($rootScope.video.duration)).toBe('00:00:52.125');
        expect(f($rootScope.video.duration, true)).toBe('00:00:52.125');
        expect(f($rootScope.video.duration, false)).toBe('00:00:52');

        expect(f(0)).toBe('00:00:00.000');
        expect(f(0, true)).toBe('00:00:00.000');
        expect(f(0, false)).toBe('00:00:00');

        expect(f('number')).toBe('');
        expect(f('number', true)).toBe('');
        expect(f('number', false)).toBe('');

        // displayZoomLevel
        var testArray = [
                [ 0, '≈ 0 s'], [ 1, '≈ 0 s'], [ 10, '≈ 0 s'], [ 1234, '≈ 1 s'], [ 12345, '≈ 12 s']
                ,[ 130000, '≈ 2 m'], [ 1700000, '≈ 28 m'], [ 1900000, '≈ 32 m'], [ 100000000, '≈ 4 h']
        ];

        for (var i = 0, len = testArray.length; i < len; i ++) {
            g(testArray[i][0]);
            expect(element.find('.zoom-control .chosen-container > a > span').html()).toBe(testArray[i][1]);
        }
    });

    describe('#getSegmentWidth', function () {

        it('returns zero without a segment', function () {
            expect(element.isolateScope().getSegmentWidth($rootScope.video.segments[1]))
                .toEqual('21.11462829736211%');
        });
    });

    describe('on player time update', function () {
        beforeEach(function () {
            $rootScope.video.duration = 329187;
            $rootScope.$digest();
        });

        it('sets the position on the time scale', function () {
            expect(element.isolateScope().positionStyle).toBe(0);
            spy();
            expect(element.isolateScope().positionStyle).toContain('15.');
        });
    });

    describe('zoom controls and interactions', function () {
        afterEach(function () {
            element.isolateScope().zoomLevel = 0;
            element.isolateScope().zoomSelected = { name: 'All', time: 0 };
            $rootScope.$digest();
        });

        it('zoom controls', function() {
            expect(element.find('.zoom-control .zoom-level').length).toBe(1);
        });

        it('zoom in - slider', function() {

            element.isolateScope().zoomLevel = 50;
            element.isolateScope().changeZoomLevel($.Event(''));
            $rootScope.$apply();

            expect(element.isolateScope().zoomLevel).toBe(50);
            expect(element.isolateScope().zoomSelected).toBe('');
            expect(element.find('.zoom-control .zoom-level').val()).toBe('50');
            var fovWidth = element.find('.field-of-vision .field').width();
            expect(fovWidth).toBeGreaterThan(59.5);
            expect(fovWidth).toBeLessThan(59.7);
        });

        it('zoom in - dropdown', function() {

            element.isolateScope().zoomSelected = { name: 'All', time: 0 };
            element.isolateScope().changeZoomSelected($.Event(''));
            $rootScope.$digest();

            expect(element.isolateScope().zoomValue).toBe(52125);
            expect(element.isolateScope().zoomOffset).toBe(0);
            expect(element.isolateScope().zoomFieldOffset).toBe(0);
            expect(element.find('.field-of-vision .field').width()).toBe(100);

            element.isolateScope().zoomSelected = { name: '1 Sec', time: 1000 };
            element.isolateScope().changeZoomSelected($.Event(''));
            $rootScope.$digest();

            expect(element.isolateScope().zoomValue).toBe(1000);
            expect(element.isolateScope().zoomOffset).toBe(0);
            expect(element.isolateScope().zoomFieldOffset).toBe(0);
            var fovWidth = element.find('.field-of-vision .field').width();
            expect(fovWidth).toBeGreaterThan(1.9);
            expect(fovWidth).toBeLessThan(2.0);
        });
    });

    describe('#mergeSegment', function () {

        beforeEach(function () {
        });

        describe('with a previous segment', function () {

            it('merges the previous with the current segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                expect($rootScope.video.segments[0].end).toEqual(17003);

                element.isolateScope().
                    mergeSegment($.Event(''), $rootScope.video.segments[1]);

                expect($rootScope.video.segments.length).toBe(2);
                expect($rootScope.video.segments[0].end).toEqual(28009);
            });
        });

        describe('without a previous but a next segment', function () {

            it('merges the next with the current segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                expect($rootScope.video.segments[0].end).toEqual(17003);

                element.isolateScope().
                    mergeSegment($.Event(''), $rootScope.video.segments[0]);

                expect($rootScope.video.segments.length).toBe(2);
                expect($rootScope.video.segments[0].end).toEqual(28009);
            });
        });

        describe('without only one segment', function () {

            beforeEach(function () {
                $rootScope.video.segments.splice(1, 2);
            });

            it('does nothing', function () {
                expect($rootScope.video.segments.length).toBe(1);
                expect($rootScope.video.segments[0].end).toEqual(17003);

                element.isolateScope().
                    mergeSegment($.Event(''), $rootScope.video.segments[0]);

                expect($rootScope.video.segments.length).toBe(1);
                expect($rootScope.video.segments[0].end).toEqual(17003);
            });
        });
    });

    /*
    describe('#move', function () {

        beforeEach(function () {
            $rootScope.video = angular.
                copy(getJSONFixture('admin-ng/tools/40518/editor.json'));
            $rootScope.$digest();
            element.find('.timeline-track').css({ width: '1000px' });
        });

        it('does nothing by default', function () {
            element.isolateScope().move($.Event(''));
            expect($rootScope.player.adapter.setCurrentTime).not.toHaveBeenCalled();
        });

        describe('while the mouse button is pressed', function () {

            beforeEach(function () {
                element.isolateScope().canMove = true;
                expect(element.isolateScope().positionStyle).toEqual(0);

                var event = $.Event('');
                event.clientX = 400;
                element.isolateScope().move(event);
            });

            it('sets the video cursor', function () {
                expect(element.isolateScope().positionStyle).toEqual('40%');
            });

            it('updates the player when the mouse button is released', function () {
                expect($rootScope.player.adapter.setCurrentTime).not.toHaveBeenCalled();
                $document.mouseup();
                expect($rootScope.player.adapter.setCurrentTime).toHaveBeenCalledWith(20.85);
            });
        });
    });

    describe('#drag', function () {

        it('allows the position marker to be moved', function () {
            expect(element.isolateScope().canMove).toBeFalsy();
            expect($document).not.toHandle('mousemove');

            element.isolateScope().drag($.Event(''));

            expect(element.isolateScope().canMove).toBeTruthy();
            expect($document).toHandle('mousemove');
        });
    });
    */

    describe('#skipToSegment', function () {

        it('sets the position marker to the given segment', function () {
            expect($rootScope.player.adapter.setCurrentTime).not.toHaveBeenCalled();

            element.isolateScope().skipToSegment($.Event(''), { start: 592001 });

            expect($rootScope.player.adapter.setCurrentTime).toHaveBeenCalledWith(592.001);
        });
    });

    describe('#selectSegment', function () {

        it('selects the given segment', function () {
            expect($rootScope.video.segments[1].selected).toBeFalsy();

            element.isolateScope().selectSegment($rootScope.video.segments[1]);

            expect($rootScope.video.segments[1].selected).toBeTruthy();
        });
    });
});
