describe('ACL controller', function () {
    var $scope, $httpBackend, AclsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$httpBackend_, $controller, _AclsResource_) {
        AclsResource = _AclsResource_;
        $scope = $rootScope.$new();
        $controller('AclsCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('acls');
    });

    describe('#delete', function () {

        it('deletes the ACL', function () {
            $httpBackend.expectDELETE('/admin-ng/acl/454').respond();
            $scope.table.delete(454);
            $httpBackend.flush();
        });
    });
});
