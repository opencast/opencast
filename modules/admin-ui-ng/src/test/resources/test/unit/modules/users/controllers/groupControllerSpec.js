describe('Group controller', function () {
    var $scope, $controller, $httpBackend, GroupResource, GroupsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _GroupResource_, _GroupsResource_) {
        $scope = $rootScope.$new();
        $scope.group = {
            $promise : {
                then : function() {
                }
            }
        };
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
        GroupResource = _GroupResource_;
        GroupsResource = _GroupsResource_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/ROLES.json?filter=role_target:USER&limit=0&offset=0').respond(JSON.stringify(getJSONFixture('roles/roles.json')));
        $httpBackend.whenGET('/admin-ng/resources/USERS.INVERSE.WITH.USERNAME.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/USERS.INVERSE.WITH.USERNAME.json')));
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
        $controller('GroupCtrl', {$scope: $scope});
        $httpBackend.flush();
    });

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
