describe('Series controller', function () {
    var $scope, $httpBackend;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_) {
        $scope = $rootScope.$new();
        $controller('SeriesCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('series');
    });


    describe('#delete', function () {

        it('deletes the series', function () {
            $httpBackend.expectGET('/admin-ng/resources/series/filters.json').respond('[]');
            $httpBackend.expectDELETE('/admin-ng/series/12').respond('12');

            $scope.table.delete(12);
 
            $httpBackend.flush();
        });
    });
});
