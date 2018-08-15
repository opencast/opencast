describe('adminNg.directives.timelineDirective', function () {
    var $compile, $httpBackend, $rootScope, $document, element, spy;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/timeline.html'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _$rootScope_, _$compile_, _$document_) {
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $rootScope = _$rootScope_;
        $document = _$document_;

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
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
        $rootScope.video = angular.copy(getJSONFixture('admin-ng/tools/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/editor.json'));
        element = $compile('<div data-admin-ng-timeline="" data-video="video" data-player="player"/></div>')($rootScope);
        element.find('.timeline-track').css({ width: '1000px' });
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

        describe('when no video duration is available', function () {
            beforeEach(function () {
                delete element.isolateScope().video.duration;
            });

            it('returns zero with no video duration', function () {
                expect(element.isolateScope().getSegmentWidth($rootScope.video.segments[1]))
                    .toEqual(0);
            });
        });

        it('returns zero without a segment', function () {
            expect(element.isolateScope().getSegmentWidth())
                .toEqual(0);
        });

        it('returns the width of a segment in percentage', function () {
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

        describe('with a next segment', function () {

            it('merges the next with the current segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                expect($rootScope.video.segments[2].start).toEqual(28009);

                element.isolateScope().
                    mergeSegment($.Event(''), $rootScope.video.segments[1]);

                expect($rootScope.video.segments.length).toBe(2);
                expect($rootScope.video.segments[1].start).toEqual(17003);
            });
        });

        describe('without a next but a previous segment', function () {

            it('merges the previous with the current segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                expect($rootScope.video.segments[2].start).toEqual(28009);

                element.isolateScope().
                    mergeSegment($.Event(''), $rootScope.video.segments[2]);

                expect($rootScope.video.segments.length).toBe(2);
                expect($rootScope.video.segments[1].start).toEqual(17003);
            });
        });

        describe('with only one segment', function () {

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

        describe('merging active segment when multipe active segments available', function () {

            var someSegment = null;

            beforeEach(function () {
                $rootScope.video.segments = $rootScope.video.segments.map(function(segment) { segment.deleted = false; return segment });
                someSegment = $rootScope.video.segments[0];
            });

            it('allows the merging of chosen active segment', function() {
                expect($rootScope.video.segments.length).toBe(3);
                element.isolateScope().mergeSegment(null, someSegment);
                expect($rootScope.video.segments.length).toBe(2);
            });
        });

        describe('merging active segment when it is the only active segment available', function () {

            var onlyActiveSegment = null;

            beforeEach(function () {
                $rootScope.video.segments = $rootScope.video.segments.map(function(segment, i) { segment.deleted = i !== 1; return segment });
                onlyActiveSegment = $rootScope.video.segments[1];
            });

            it('does not allow the removal of the only available active segment', function() {
                expect($rootScope.video.segments.length).toBe(3);
                element.isolateScope().mergeSegment(null, onlyActiveSegment);
                expect($rootScope.video.segments.length).toBe(3);
            });
        });
    });

    describe('#move', function () {

        beforeEach(function () {
            $rootScope.video = angular.
                copy(getJSONFixture('admin-ng/tools/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/editor.json'));
            $rootScope.$digest();
            element.find('.timeline-track').css({ width: '1000px' });
        });

        it('does nothing by default', function () {
            element.isolateScope().moveSegment($.Event('mousemove'));
            expect($rootScope.player.adapter.setCurrentTime).not.toHaveBeenCalled();
        });

        describe('while the mouse button is pressed', function () {

            var handle = null;

            beforeEach(function () {
                element.isolateScope().canMove = true;
                handle = element.find('#cursor')[0];
                $document.triggerHandler({
                  type: 'mousemove',
                  pageX: 0,
                  pageY: 0
                });
                $(handle).triggerHandler({
                    type: 'mousedown',
                    pageX: 0,
                    pageY: 0
                });
                $document.triggerHandler({
                  type: 'mousemove',
                  pageX: 400,
                  pageY: 0
                });
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

        var handle = null;

        beforeEach(function () {
            handle = element.find('#cursor')[0];
        });

        it('allows the position marker to be moved', function () {
            expect(element.isolateScope().canMove).toBeFalsy();
            $(handle).triggerHandler({
                type: 'mousedown'
            });
            expect(element.isolateScope().canMove).toBeTruthy();
        });
    });

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

    describe('adjust segment lengths using timeline', function () {

        var separator = null;

        beforeEach(function () {
            element.find('.timeline-track .segments').css({ width: '1000px' });
            element.find('.timeline-track div:nth-of-type(2)').css({ width: '1000px' });
            separator = element.find('.segment-seperator')[1];
            $document.triggerHandler({
                type: 'mousemove',
                pageX: 0,
                pageY: 0
            });
        });

        it('does nothing by default', function () {
            expect(element.isolateScope().movingSegment).toBeFalsy();
        });

        describe('when mouse button is pressed', function () {

            it('allows the segment to be resized', function () {
                expect(element.isolateScope().movingSegment).toBeFalsy();
                $(separator).triggerHandler({
                    type: 'mousedown',
                    currentTarget: separator,
                });
                expect(element.isolateScope().movingSegment).toBeTruthy();
            });
        });

        describe('when segment is adjusted', function () {

            it('sets segment durations as expected', function () {
                expect($rootScope.video.segments[0].end).toBe(17003);
                expect($rootScope.video.segments[1].start).toBe(17003);
                var movePixels = 400;
                $(separator).triggerHandler({
                    type: 'mousedown',
                    currentTarget: separator,
                    pageX: 0,
                    pageY: 0
                });
                $document.triggerHandler({
                    type: 'mousemove',
                    pageX: movePixels,
                    pageY: 0
                });
                $document.triggerHandler({
                    type: 'mouseup',
                });

                var timeFromPixels = $rootScope.video.duration * (movePixels + 3) / 1000 >> 0;
                expect($rootScope.video.segments[0].end).toBe(timeFromPixels);
                expect($rootScope.video.segments[1].start).toBe(timeFromPixels);
            });
        });

        describe('when deactivated segment is adjusted to overlap singly available active segment', function() {

            beforeEach(function () {
                $rootScope.video.segments = $rootScope.video.segments.map(function(segment, i) { segment.deleted = i !== 1; return segment });
                separator = element.find('.segment-seperator')[2];
            });

            it('does not remove only active segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                $(separator).triggerHandler({
                    type: 'mousedown',
                    currentTarget: separator,
                    pageX: 0,
                    pageY: 0
                });
                $document.triggerHandler({
                    type: 'mousemove',
                    pageX: -230,
                    pageY: 0
                });
                $document.triggerHandler({
                    type: 'mouseup',
                });
                expect($rootScope.video.segments.length).toEqual(3);
                expect($rootScope.video.segments[1].end - $rootScope.video.segments[1].start).toEqual(1);
            });
        });

        describe('when one of multiple active segments is overlapped', function () {

            var lastSeparator = null;

            beforeEach(function () {
                $rootScope.video.segments = $rootScope.video.segments.map(function(segment, i) { segment.deleted = i === 0; return segment });
                lastSeparator = element.find('.segment-seperator')[2];
                $document.triggerHandler({
                    type: 'mousemove',
                    pageX: 300,
                    pageY: 0
                });
            });

            it('removes the overlapped active segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                $(lastSeparator).triggerHandler({
                    type: 'mousedown',
                    currentTarget: lastSeparator,
                    pageX: 0,
                    pageY: 0
                });
                $document.triggerHandler({
                    type: 'mousemove',
                    pageX: 0,
                    pageY: 0
                });
                $document.triggerHandler({
                    type: 'mouseup',
                });
                expect($rootScope.video.segments.length).toBe(2);
            });
        });
    });

    describe('toggle segment', function () {

        var segmentEls = null, $scope = null;

        beforeEach(function () {
            segmentEls = element.find('.segment');
            $scope = element.isolateScope();
            $scope.video.segments = $scope.video.segments.map(function (segment) { segment.deleted = true; return segment; } );
            spyOn($scope, 'toggleSegment').and.callThrough();
            spyOn($scope, 'isRemovalAllowed').and.callThrough();
        //    spyOn($scope, 'isRemovalAllowed');
        });

        it('activates a deactivated segment', function () {
            $(segmentEls[1]).find('a:nth-of-type(2)').click();
            expect($scope.toggleSegment).toHaveBeenCalledWith(jasmine.any(Object), $scope.video.segments[1]);
            expect($scope.video.segments[1].deleted).toEqual(false);
        });

        describe('when clicking an activated segment', function () {

            describe('when multiple active segments are available', function () {
                beforeEach(function () {
                    $scope.video.segments[0].deleted = false;
                    $scope.video.segments[1].deleted = false;
                });

                it('deactivates an activated segment', function () {
                    $(segmentEls[1]).find('a:nth-of-type(2)').click();
                    expect($scope.toggleSegment).toHaveBeenCalledWith(jasmine.any(Object), $scope.video.segments[1]);
                    expect($scope.isRemovalAllowed).toHaveBeenCalledWith($scope.video.segments[1]);
                    expect($scope.video.segments[1].deleted).toEqual(true);
                });
            });

            describe('when only one active segment is available', function () {
                beforeEach(function () {
                    $scope.video.segments[1].deleted = true;
                });

                it('does not deactivate the only active segment', function () {
                    $(segmentEls[1]).find('a:nth-of-type(2)').click();
                    expect($scope.toggleSegment).toHaveBeenCalledWith(jasmine.any(Object), $scope.video.segments[1]);
                    expect($scope.isRemovalAllowed).toHaveBeenCalledWith($scope.video.segments[1]);
                    expect($scope.video.segments[1].deleted).toEqual(false);
                });
            });
        });
    });

    describe('checking the removal status of non-segment', function () {

        it('does nothing', function () {
            expect(element.isolateScope().isRemovalAllowed()).toBeFalsy();
        });
    });
});
