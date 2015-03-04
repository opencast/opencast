describe('Events controller', function () {
    var $scope, EventsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _EventsResource_) {
        $scope = $rootScope.$new();
        EventsResource = _EventsResource_;
        $controller('EventsCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('events');
    });

    describe('#delete', function () {

        it('deletes the event', function () {
            var event = { $delete: jasmine.createSpy() };
            $scope.table.delete(event);

            expect(event.$delete).toHaveBeenCalled();
        });
    });
});
