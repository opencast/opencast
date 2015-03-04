describe('Users controller', function () {
    var $scope, UsersResource, UserResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _UsersResource_, _UserResource_) {
        $scope = $rootScope.$new();
        UsersResource = _UsersResource_;
        UserResource = _UserResource_;
        $controller('UsersCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('users');
    });

    describe('#delete', function () {

        it('deletes the user', function () {
            var user = {};
            spyOn(UserResource, 'delete');

            $scope.table.delete(user);

            expect(UserResource.delete).toHaveBeenCalled();
        });
    });
});
