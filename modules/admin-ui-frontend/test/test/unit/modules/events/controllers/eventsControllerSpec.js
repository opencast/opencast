describe('Events controller', function () {
    var $scope, $httpBackend, EventsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _EventsResource_) {
        $scope = $rootScope.$new();
        $controller('EventsCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
        EventsResource = _EventsResource_;
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('events');
    });

    describe('#delete', function () {

        it('deletes the event', function () {
            spyOn(EventsResource, 'delete');
            $scope.table.delete({'id': 12});
            expect(EventsResource.delete).toHaveBeenCalledWith({id: 12}, jasmine.any(Function), jasmine.any(Function));
        });

        it('reloads events after deletion', function () {
            $httpBackend.expectGET('/admin-ng/resources/STATS.json').respond('[]');
            $httpBackend.expectGET('/admin-ng/resources/events/filters.json').respond('[]');
            $httpBackend.expectGET('/admin-ng/resources/PUBLICATION.CHANNELS.json').respond('{}');
            $httpBackend.expectDELETE('/admin-ng/event/12').respond('12');
            $httpBackend.whenGET('/admin-ng/event/events.json?limit=10&offset=0&sort=title:ASC').respond(JSON.stringify(getJSONFixture('admin-ng/event/events.json')));

            $scope.table.delete({'id': 12});

            $httpBackend.flush();
        });

    });
});
