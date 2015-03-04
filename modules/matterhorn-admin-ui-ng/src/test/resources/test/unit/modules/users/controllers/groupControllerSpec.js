describe('Group controller', function () {
    var $scope, $controller, GroupResource, GroupsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _GroupResource_, _GroupsResource_) {
        $scope = $rootScope.$new();
        $controller = _$controller_;
        GroupResource = _GroupResource_;
        GroupsResource = _GroupsResource_;
        $controller('GroupCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.user).toBeDefined();
    });

    it('sets the creation caption', function () {
        expect($scope.caption).toContain('NEWCAPTION');
    });

    describe('when editing', function () {

        it('sets the edit caption', function () {
            $scope.action = 'edit';
            $controller('GroupCtrl', {$scope: $scope});

            expect($scope.caption).toContain('EDITCAPTION');
        });
    });

    describe('#submit', function () {

        beforeEach(function () {
            $scope.group = { id: 'admins' };
            spyOn(GroupsResource, 'create');
            $scope.submit();
        });

        it('saves the group record', function () {
            expect(GroupsResource.create).toHaveBeenCalledWith({ id : 'admins', users : [  ], roles : [  ] }, jasmine.any(Function), jasmine.any(Function));
        });
    });
});
