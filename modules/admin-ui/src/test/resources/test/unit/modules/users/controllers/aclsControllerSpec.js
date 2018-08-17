describe('ACL controller', function () {
    var $scope, $httpBackend, AclsResource, AclResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$httpBackend_, $controller, _AclsResource_, _AclResource_) {
        $scope = $rootScope.$new();
        $controller('AclsCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
        AclsResource = _AclsResource_;
        AclResource = _AclResource_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('acls');
    });

    describe('#delete', function () {

        it('deletes the ACL', function () {
            spyOn(AclResource, 'delete');
            $scope.table.delete({'id': 454});
            expect(AclResource.delete).toHaveBeenCalledWith({id: 454}, jasmine.any(Function), jasmine.any(Function));
        });

        it('reloads acls after deletion', function () {
            $httpBackend.expectGET('/admin-ng/resources/acls/filters.json').respond('[]');
            $httpBackend.expectDELETE('/admin-ng/acl/454').respond();
            $httpBackend.expectGET('/admin-ng/acl/acls.json?limit=10&offset=0&sort=name:ASC').respond(JSON.stringify(getJSONFixture('admin-ng/acl/acls.json')));

            $scope.table.delete({'id': 454});

            $httpBackend.flush();
        });

    });
});
