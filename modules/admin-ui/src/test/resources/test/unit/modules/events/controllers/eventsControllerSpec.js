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
            $httpBackend.expectGET('/admin-ng/resources/events/filters.json').respond('[]');
            $httpBackend.expectGET('/admin-ng/resources/PUBLICATION.CHANNELS.json').respond('{}');
            $httpBackend.expectDELETE('/admin-ng/event/12').respond('12');
            $httpBackend.expectGET('/admin-ng/event/events.json?limit=10&offset=0').respond(JSON.stringify(getJSONFixture('admin-ng/event/events.json')));

            $scope.table.delete({'id': 12});

            $httpBackend.flush();
        });

    });
});
