describe('Users controller', function () {
    var $scope, $httpBackend, UsersResource, UserResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _UsersResource_, _UserResource_) {
        $scope = $rootScope.$new();
        $controller('UsersCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
        UsersResource = _UsersResource_;
        UserResource = _UserResource_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('users');
    });

    describe('#delete', function () {

        it('deletes the user', function () {
            spyOn(UserResource, 'delete');
            $scope.table.delete({'id': 12});
            expect(UserResource.delete).toHaveBeenCalledWith({username: 12}, jasmine.any(Function), jasmine.any(Function));
        });

    });
});
