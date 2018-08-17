describe('Servers controller', function () {
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
        $controller('ServersCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('servers');
    });

    describe('#setMaintenanceMode', function () {
        beforeEach(function () {
            $httpBackend.whenGET('/admin-ng/server/servers.json?limit=10&offset=0')
                .respond(JSON.stringify(getJSONFixture('admin-ng/server/servers.json')));
            $httpBackend.whenGET('/admin-ng/resources/servers/filters.json').respond('{}');
            spyOn($scope.table, 'fetch');
        });

        it('sets maintenance mode', function () {
            $httpBackend.expectPOST('/services/maintenance', function (data) {
                expect($.deparam(data)).toEqual({
                    host: 'host3',
                    maintenance: 'true'
                });
                return true;
            }).respond(204);
            $scope.table.setMaintenanceMode('host3', true);
            $httpBackend.flush();
            expect($scope.table.fetch).toHaveBeenCalled();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenPOST('/services/maintenance').respond(204);
            });

            it('updates the table', function () {
                $scope.table.setMaintenanceMode('host3', true);
                $httpBackend.flush();
                expect($scope.table.fetch).toHaveBeenCalled();
            });
        });
    });
});
