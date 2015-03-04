describe('Series controller', function () {
    var $scope;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller) {
        $scope = $rootScope.$new();
        $controller('SeriesCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('series');
    });
});
