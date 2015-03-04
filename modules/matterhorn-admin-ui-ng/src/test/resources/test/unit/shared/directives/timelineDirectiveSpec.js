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

    describe('#mergeSegment', function () {

        beforeEach(function () {
        });

        describe('with a previous segment', function () {

            it('merges the previous with the current segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                expect($rootScope.video.segments[0].end).toEqual(17003);

                element.isolateScope().
                    mergeSegment($rootScope.video.segments[1]);

                expect($rootScope.video.segments.length).toBe(2);
                expect($rootScope.video.segments[0].end).toEqual(28009);
            });
        });

        describe('without a previous but a next segment', function () {

            it('merges the next with the current segment', function () {
                expect($rootScope.video.segments.length).toBe(3);
                expect($rootScope.video.segments[0].end).toEqual(17003);

                element.isolateScope().
                    mergeSegment($rootScope.video.segments[0]);

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
                    mergeSegment($rootScope.video.segments[0]);

                expect($rootScope.video.segments.length).toBe(1);
                expect($rootScope.video.segments[0].end).toEqual(17003);
            });
        });
    });

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

        it('allows the position markre to be moved', function () {
            expect(element.isolateScope().canMove).toBeFalsy();
            expect($document).not.toHandle('mousemove');

            element.isolateScope().drag($.Event(''));

            expect(element.isolateScope().canMove).toBeTruthy();
            expect($document).toHandle('mousemove');
        });
    });

    describe('#skipToSegment', function () {

        it('sets the position marker to the given segment', function () {
            expect($rootScope.player.adapter.setCurrentTime).not.toHaveBeenCalled();

            element.isolateScope().skipToSegment({ start: 592001 });

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
