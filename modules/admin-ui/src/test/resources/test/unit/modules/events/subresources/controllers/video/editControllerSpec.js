describe('Video Edit controller', function () {
    var $scope, $controller, $parentScope;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
        $provide.value('$route', {
            current: {
                params: {
                    itemId: 5681
                }
            }
        });
    }));

    beforeEach(inject(function ($rootScope, _$controller_) {
        $controller = _$controller_;

        $parentScope = $rootScope.$new();
        $scope = $parentScope.$new();

        $controller('VideoEditCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $scope.video = angular.copy(getJSONFixture('admin-ng/tools/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/editor.json'));
        $scope.player = { adapter: {} };
    });

    it('instantiates', function () {
        expect($scope.video).toBeDefined();
        expect($scope.video.duration).toBeDefined();
    });

    describe('#split', function () {

        beforeEach(function () {
            $scope.player.adapter = {
                getCurrentTime: function () { return 10.821; }
            };
        });

        it('creates a subset for each segment at the current cursor', function () {
            expect($scope.video.segments.length).toBe(3);
            $scope.split();
            expect($scope.video.segments.length).toBe(4);
        });

        it('does not create overlaps', function () {
            $scope.split();
            expect($scope.video.segments[0]).toEqual({ start: 0, end: 10821 });
            expect($scope.video.segments[1]).toEqual({ start: 10821, end: 17003 });
            expect($scope.video.segments[2]).toEqual({ start: 17003, end: 28009 });
            expect($scope.video.segments[3]).toEqual({ start: 28009, end: 52125 });
        });
    });

    describe('#clearSelectedSegment', function () {

        it('removes the selected / middle segment', function () {
            expect($scope.video.segments.length).toBe(3);
            $scope.video.segments[1].selected = true;
            $scope.clearSelectedSegment();
            expect($scope.video.segments.length).toBe(2);
        });
    });

    describe('#clearSegments', function () {

        it('Remove the middle segment', function () {
            expect($scope.video.segments.length).toBe(3);
            $scope.clearSegments();
            expect($scope.video.segments.length).toBe(1);
        });

        it('scales the remaining segment to the size of the video', function () {
            $scope.clearSegments();
            expect($scope.video.segments[0].start).toBe(0);
            expect($scope.video.segments[0].end).toEqual($scope.video.duration);
        });
    });

    describe('#cut', function () {

        it('marks the current segment for deletion', function () {
            expect($scope.video.segments[0].deleted).toBeFalsy();

            $scope.video.segments[0].selected = true;
            $scope.cut();

            expect($scope.video.segments[0].deleted).toBeTruthy();
        });
    });
});
