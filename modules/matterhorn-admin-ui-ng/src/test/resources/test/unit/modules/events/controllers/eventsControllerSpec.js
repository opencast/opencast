describe('Events controller', function () {
    var $scope, EventsResource, $httpBackend;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _EventsResource_, _$httpBackend_) {
        $scope = $rootScope.$new();
        EventsResource = _EventsResource_;
        $controller('EventsCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('events');
    });

    describe('#delete', function () {

        it('deletes the event', function () {
            $httpBackend.expectGET('/admin-ng/resources/events/filters.json').respond('[]');
            $httpBackend.expectDELETE('/admin-ng/event/12').respond('12');

            $scope.table.delete(12);
 
            $httpBackend.flush();
        });
    });
});
