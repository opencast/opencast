describe('Groups controller', function () {
    var $scope, GroupsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _GroupsResource_) {
        $scope = $rootScope.$new();
        GroupsResource = _GroupsResource_;
        $controller('GroupsCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('groups');
    });

    describe('#delete', function () {

        it('deletes the group', function () {
            var group = { $delete: jasmine.createSpy() };
            $scope.table.delete(group);

            //expect(group.$delete).toHaveBeenCalled();
        });
    });
});
