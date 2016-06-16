describe('Tools controller', function () {
    var $scope;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        },
        routeMock = {
            current: {
                params: {
                    category:    'events',
                    resource:    'events',
                    itemId:      '30121',
                    subresource: 'tools',
                    tab:         'tab-1'
                }
            }
        };
        $provide.value('Language', service);
        $provide.value('$route', routeMock);
    }));

    beforeEach(inject(function ($rootScope, $controller) {
        $scope = $rootScope.$new();
        $controller('ToolsCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.resource).toEqual('events');
        expect($scope.tab).toEqual('tab-1');
    });
});
