describe('adminNg.directives.segmentsDirective', function () {
    var $httpBackend, $compile, $rootScope, $document, ToolsResource, element, spy;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/segments.html'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _$rootScope_, _$compile_, _$document_, _ToolsResource_) {
        $httpBackend  = _$httpBackend_;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $document = _$document_;
        ToolsResource = _ToolsResource_;
    }));

    beforeEach(function () {
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
        $rootScope.setChanges = function() {};
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.expectGET('/admin-ng/tools/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/editor.json').respond(JSON.stringify(
            getJSONFixture('admin-ng/tools/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/editor.json')));
        $rootScope.video =  ToolsResource.get({ id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a', tool: 'editor' });
        $httpBackend.flush();
        element = $compile('<div data-admin-ng-segments="" data-video="video" data-player="player"/></div>')($rootScope);
        $rootScope.$digest();
    });

    describe('displays segment information', function () {

        var segmentEls = null, inputEls = null;

        beforeEach(function () {
            segmentEls = element.find('#segments .segment-list-entry');
            inputEls = element.find('input');
        });

        it('renders required elements in relevant container', function () {
            expect(element).toContainElement('#segments');
            expect(segmentEls.length).toBe(3);

            expect(inputEls[0].value).toBe('00:00:00.000');
            expect(inputEls[1].value).toBe('00:00:17.003');
            expect(inputEls[2].value).toBe('00:00:17.003');
            expect(inputEls[3].value).toBe('00:00:28.009');
            expect(inputEls[4].value).toBe('00:00:28.009');
            expect(inputEls[5].value).toBe('00:00:52.125');
        });
    });

    describe('toggle segment', function () {

        var segmentEls = null, inputEls = null, scope = null;

        beforeEach(function () {
            segmentEls = element.find('#segments .segment-list-entry');
            inputEls = element.find('input');
            scope = element.isolateScope();
        });

        it('does nothing with a non-segment', function () {
            expect(scope.toggleSegment()).toBeFalsy();
            expect(scope.toggleSegment($.Event(''), {})).toBeFalsy();
        });

        describe('when toggling a deactivated segment', function () {

            beforeEach(function () {
                scope.video.segments[1].deleted = true;
                spyOn(scope, 'toggleSegment').and.callThrough();
            });

            it('activates a deactivated segment', function () {
                $(segmentEls[1]).find('div:nth-of-type(1) a:nth-of-type(1)').click();
                expect(scope.toggleSegment).toHaveBeenCalledWith(jasmine.any(Object), scope.video.segments[1]);
                expect(scope.video.segments[1].deleted).toBe(false);
            });
        });

        describe('when toggling an active segment', function () {

            beforeEach(function () {
                spyOn(scope, 'toggleSegment').and.callThrough();
                spyOn(scope, 'isRemovalAllowed').and.callThrough();
                scope.video.segments = scope.video.segments.map(function (segment) { segment.deleted = false; return segment; });
            });

            it('deactivates an activated segment', function () {
                $(segmentEls[1]).find('div:nth-of-type(1) a:nth-of-type(1)').click();
                expect(scope.toggleSegment).toHaveBeenCalledWith(jasmine.any(Object), scope.video.segments[1]);
                expect(scope.video.segments[1].deleted).toBe(true);
            });

            describe('when only one active segment is available', function () {

                beforeEach(function () {
                    scope.video.segments = scope.video.segments.map(function (segment, i) { segment.deleted = i !== 1; return segment; });
                });

                it('does not deactivate only available active segment', function () {
                    $(segmentEls[1]).find('div:nth-of-type(1) a:nth-of-type(1)').click();
                    expect(scope.toggleSegment).toHaveBeenCalledWith(jasmine.any(Object), scope.video.segments[1]);
                    expect(scope.isRemovalAllowed).toHaveBeenCalledWith(scope.video.segments[1]);
                    expect(scope.video.segments[1].deleted).toBe(false);
                });
            });

        });
    });

    describe('adjust segment start time', function () {

        var segmentEls = null, inputEls = null, scope = null, targetInput = null;

        beforeEach(function () {
            segmentEls = element.find('#segments .segment-list-entry');
            inputEls = element.find('input');
            targetInput = inputEls[2];
            scope = element.isolateScope();
            spyOn(scope, 'isRemovalAllowed').and.callThrough();
            spyOn(scope, 'updateStartTime').and.callThrough();
            spyOn(scope, 'timeValid').and.callThrough();
            spyOn(scope, 'parseTime').and.callThrough();
            scope.$digest();
            scope.video.segments = scope.video.segments.map(function (segment) { segment.deleted = false; return segment; });
        });

        it('does nothing when start time exceeds video duration', function () {
            $(targetInput).val('00:01:00.000');
            $(targetInput).trigger('change');
            expect(scope.updateStartTime).toHaveBeenCalled();
            expect(scope.parseTime).toHaveBeenCalledWith('00:01:00.000');
            expect(scope.video.segments[1].startTime).toBe('00:00:17.003');
            expect(scope.video.segments[1].start).toEqual(17003);
        });

        it('updates relevant segments on valid input', function () {
            $(targetInput).val('00:00:25.000');
            $(targetInput).trigger('change');
            expect(scope.updateStartTime).toHaveBeenCalled();
            expect(scope.video.segments[1].startTime).toBe('00:00:25.000');
            expect(scope.video.segments[1].start).toBe(25000);
            expect(scope.video.segments[0].endTime).toBe('00:00:25.000');
            expect(scope.video.segments[0].end).toEqual(25000);
        });

        describe("when setting segment start time to earlier than previous segment's start time", function () {
            beforeEach(function () {
                targetInput = inputEls[4];
            });

            it('removes previous segment if new start time preceeds it', function () {

                $(targetInput).val('00:00:10.000');
                $(targetInput).trigger('change');
                expect(scope.updateStartTime).toHaveBeenCalled();
                expect(scope.video.segments.length).toEqual(2);
                expect(scope.video.segments[1].startTime).toBe('00:00:10.000');
                expect(scope.video.segments[1].start).toEqual(10000);
                expect(scope.video.segments[0].endTime).toBe('00:00:10.000');
                expect(scope.video.segments[0].end).toEqual(10000);
            });

            describe("when previous segment is the only active segment", function () {
                beforeEach(function () {
                    scope.video.segments = scope.video.segments.map(function (segment, i) { segment.deleted = i !== 1; return segment });
                });

                it('does not remove previous segment', function () {

                    $(targetInput).val('00:00:10.000');
                    $(targetInput).trigger('change');
                    expect(scope.updateStartTime).toHaveBeenCalled();
                    expect(scope.video.segments.length).toEqual(3);
                    expect(scope.video.segments[2].startTime).toBe('00:00:28.009');
                    expect(scope.video.segments[2].start).toEqual(28009);
                    expect(scope.video.segments[1].endTime).toBe('00:00:28.009');
                    expect(scope.video.segments[1].end).toEqual(28009);
                });
            });
        });
    });

    describe('adjust segment end time', function () {

        var segmentEls = null, inputEls = null, scope = null, targetInput = null;

        beforeEach(function () {
            segmentEls = element.find('#segments .segment-list-entry');
            inputEls = element.find('input');
            targetInput = inputEls[3];
            scope = element.isolateScope();
            spyOn(scope, 'isRemovalAllowed').and.callThrough();
            spyOn(scope, 'updateEndTime').and.callThrough();
            spyOn(scope, 'timeValid').and.callThrough();
            spyOn(scope, 'parseTime').and.callThrough();
            scope.$digest();
            scope.video.segments = scope.video.segments.map(function (segment) { segment.deleted = false; return segment; });
        });

        it('does nothing when start time exceeds video duration', function () {
            $(targetInput).val('00:01:00.000');
            $(targetInput).trigger('change');
            expect(scope.updateEndTime).toHaveBeenCalled();
            expect(scope.parseTime).toHaveBeenCalledWith('00:01:00.000');
            expect(scope.video.segments[1].endTime).toBe('00:00:28.009');
            expect(scope.video.segments[1].end).toEqual(28009);
        });

        it('updates relevant segments on valid input', function () {
            $(targetInput).val('00:00:25.000');
            $(targetInput).trigger('change');
            expect(scope.updateEndTime).toHaveBeenCalled();
            expect(scope.video.segments[1].endTime).toBe('00:00:25.000');
            expect(scope.video.segments[1].end).toBe(25000);
            expect(scope.video.segments[2].startTime).toBe('00:00:25.000');
            expect(scope.video.segments[2].start).toEqual(25000);
        });

        describe("when setting segment end time to later than next segment's end time", function () {
            beforeEach(function () {
                targetInput = inputEls[1];
            });

            it('removes previous segment if new start time preceeds it', function () {

                $(targetInput).val('00:00:30.000');
                $(targetInput).trigger('change');
                expect(scope.updateEndTime).toHaveBeenCalled();
                expect(scope.video.segments.length).toEqual(2);
                expect(scope.video.segments[0].endTime).toBe('00:00:30.000');
                expect(scope.video.segments[0].end).toEqual(30000);
                expect(scope.video.segments[1].startTime).toBe('00:00:30.000');
                expect(scope.video.segments[1].start).toEqual(30000);
            });

            describe("when next segment is the only active segment", function () {
                beforeEach(function () {
                    scope.video.segments = scope.video.segments.map(function (segment, i) { segment.deleted = i !== 1; return segment });
                });

                it('does not remove next segment', function () {

                    $(targetInput).val('00:00:30.000');
                    $(targetInput).trigger('change');
                    expect(scope.updateEndTime).toHaveBeenCalled();
                    expect(scope.video.segments.length).toEqual(3);
                    expect(scope.video.segments[1].startTime).toBe('00:00:17.003');
                    expect(scope.video.segments[1].start).toEqual(17003);
                    expect(scope.video.segments[0].endTime).toBe('00:00:17.003');
                    expect(scope.video.segments[0].end).toEqual(17003);
                });
            });
        });
    });

    describe('merge segment', function () {

        var segmentEls = null, scope = null;

        beforeEach(function () {
            segmentEls = element.find('#segments .segment-list-entry');
            inputEls = element.find('input');
            targetInput = inputEls[3];
            scope = element.isolateScope();
            spyOn(scope, 'isRemovalAllowed').and.callThrough();
            spyOn(scope, 'mergeSegment').and.callThrough();
            scope.$digest();
            scope.video.segments = scope.video.segments.map(function (segment) { segment.deleted = false; return segment; });
        });

        it('removes the chosen segment', function () {
            var cacheSegment = scope.video.segments[1];
            $(segmentEls[1]).find('> a').click();
            expect(scope.mergeSegment).toHaveBeenCalledWith(jasmine.any(Object), cacheSegment);
            expect(scope.video.segments.length).toEqual(2);
        });

        describe('merging the only active segment', function () {

            beforeEach(function () {
                scope.video.segments = scope.video.segments.map(function (segment, i) { segment.deleted = i !== 1; return segment; });
            });

            it('does not merge the only active segment', function () {
                $(segmentEls[1]).find('> a').click();
                expect(scope.mergeSegment).toHaveBeenCalledWith(jasmine.any(Object), scope.video.segments[1]);
                expect(scope.isRemovalAllowed).toHaveBeenCalledWith(scope.video.segments[1]);
                expect(scope.video.segments.length).toEqual(3);
            });
        });
    });

    describe('skip to segment', function () {

        var segmentEls = null, scope = null;

        beforeEach(function () {
            segmentEls = element.find('#segments .segment-list-entry');
            scope = element.isolateScope();
            spyOn(scope, 'skipToSegment').and.callThrough();
            scope.$digest();
            scope.video.segments = scope.video.segments.map(function (segment) { segment.selected = false; return segment; });
        });

        it('focuses on segment', function () {
            $(segmentEls[2]).click();
            expect(scope.skipToSegment).toHaveBeenCalledWith(jasmine.any(Object), scope.video.segments[2]);
        });
    });
});
