describe('Groups controller', function () {
    var $scope, $httpBackend, GroupsResource, GroupResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _GroupsResource_, _GroupResource_) {
        $scope = $rootScope.$new();
        $controller('GroupsCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
        GroupsResource = _GroupsResource_;
        GroupResource = _GroupResource_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('groups');
    });

    describe('#delete', function () {

        it('deletes the group', function () {
            spyOn(GroupResource, 'delete');
            $scope.table.delete({'id': 12});
            expect(GroupResource.delete).toHaveBeenCalledWith({id: 12}, jasmine.any(Function), jasmine.any(Function));
        });

    });
});
