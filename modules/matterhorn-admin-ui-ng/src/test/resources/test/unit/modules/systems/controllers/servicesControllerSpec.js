describe('Services controller', function () {
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
        $controller('ServicesCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('services');
    });

    describe('#sanitize', function () {
        beforeEach(function () {
            spyOn($scope.table, 'fetch');
        });

        it('sanitizes the service', function () {
            $httpBackend.expectPOST('/services/sanitize', function (data) {
                expect($.deparam(data)).toEqual({
                    host: 'host3',
                    serviceType: 'CAPTURE'
                });
                return true;
            }).respond(204);
            $scope.table.sanitize('host3', 'CAPTURE');
            $httpBackend.flush();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenPOST('/services/sanitize').respond(204);
            });

            it('updates the table', function () {
                $scope.table.sanitize('host3', 'CAPTURE');
                $httpBackend.flush();
                expect($scope.table.fetch).toHaveBeenCalled();
            });
        });
    });
});
