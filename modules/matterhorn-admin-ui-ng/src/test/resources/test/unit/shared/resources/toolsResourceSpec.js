describe('Tools API Resource', function () {
    var ToolsResource, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _ToolsResource_) {
        $httpBackend  = _$httpBackend_;
        ToolsResource = _ToolsResource_;
    }));

    describe('#get', function () {

        it('selects the first segment', function () {
            $httpBackend.expectGET('/admin-ng/tools/287/editor.json').
                respond(JSON.stringify({
                    segments: [{
                        start: 0,
                        end:   1000
                    }, {
                        start: 1001,
                        end:   2000
                    }]
                }));
            var result = ToolsResource.get({ id: 287, tool: 'editor' });
            $httpBackend.flush();

            expect(result.segments.length).toBe(2);
            expect(result.segments[0].selected).toBeTruthy();
            expect(result.segments[1].selected).toBeFalsy();
        });

        it('creates a default segment', function () {
            $httpBackend.expectGET('/admin-ng/tools/287/editor.json').
                respond(JSON.stringify({
                    segments: [],
                    duration: 38101
                }));
            var result = ToolsResource.get({ id: 287, tool: 'editor' });
            $httpBackend.flush();

            expect(result.segments.length).toBe(1);
            expect(result.segments[0]).toEqual({ start: 0, end: 38101, selected: true });
        });

        it('sorts segments by their start attribute', function () {
            $httpBackend.expectGET('/admin-ng/tools/287/editor.json').
                respond(JSON.stringify({
                    segments: [{
                        start: 1000,
                        end:   2000
                    }, {
                        start: 0,
                        end:   1000
                    }],
                    duration: 2000
                }));
            var result = ToolsResource.get({ id: 287, tool: 'editor' });
            $httpBackend.flush();

            expect(result.segments.length).toBe(2);
            expect(result.segments[0]).toEqual({ start: 0, end: 1000, selected: true });
            expect(result.segments[1]).toEqual({ start: 1000, end: 2000 });
        });

        it('adds an empty workflow option', function () {
            $httpBackend.expectGET('/admin-ng/tools/287/editor.json').
                respond(JSON.stringify({
                    segments: [],
                    workflows: [{ name: 'Workflow 1', id: 'wf1' }]
                }));
            var result = ToolsResource.get({ id: 287, tool: 'editor' });
            $httpBackend.flush();

            expect(result.workflows.length).toBe(2);
            expect(result.workflows[0].name).toEqual('No Workflow');
            expect(result.workflows[1].name).toEqual('Workflow 1');
        });

        describe('noncontinous segments', function () {

            it('fills gaps at the borders', function () {
                $httpBackend.expectGET('/admin-ng/tools/287/editor.json').
                    respond(JSON.stringify({
                        segments: [{
                            start: 500,
                            end:   1000
                        }, {
                            start: 1000,
                            end:   2000
                        }],
                        duration: 3000
                    }));
                var result = ToolsResource.get({ id: 287, tool: 'editor' });
                $httpBackend.flush();

                expect(result.segments.length).toBe(4);
                expect(result.segments[0])
                    .toEqual({ start: 0, end: 500, deleted: true, selected: true });
                expect(result.segments[1])
                    .toEqual({ start: 500, end: 1000 });
                expect(result.segments[2])
                    .toEqual({ start: 1000, end: 2000 });
                expect(result.segments[3])
                    .toEqual({ start: 2000, end: 3000, deleted: true });
            });

            it('fills gaps between segments', function () {
                $httpBackend.expectGET('/admin-ng/tools/287/editor.json').
                    respond(JSON.stringify({
                        segments: [{
                            start: 0,
                            end:   1000
                        }, {
                            start: 2000,
                            end:   3000
                        }],
                        duration: 3000
                    }));
                var result = ToolsResource.get({ id: 287, tool: 'editor' });
                $httpBackend.flush();

                expect(result.segments.length).toBe(3);
                expect(result.segments[0])
                    .toEqual({ start: 0, end: 1000, selected: true });
                expect(result.segments[1])
                    .toEqual({ start: 1000, end: 2000, deleted: true });
                expect(result.segments[2])
                    .toEqual({ start: 2000, end: 3000 });
            });
        });
    });

    describe('#save', function () {

        it('calls the tools service', function () {
            $httpBackend.expectPOST('/admin-ng/tools/287/editor.json').respond(200);
            ToolsResource.save({ id: 287, tool: 'editor' }, {
                segments: [{
                    start: 0,
                    end: 1000
                }],
                tracks: [{
                    name: 'audio',
                    id: 'audio-1'
                }, {
                    name: 'video',
                    id: 'video-1'
                }]
            });
            $httpBackend.flush();
        });
    });
});
