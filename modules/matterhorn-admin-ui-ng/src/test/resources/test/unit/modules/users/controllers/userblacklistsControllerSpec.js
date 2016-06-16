describe('User Blacklist controller', function () {
    var $scope, $httpBackend, Notifications, Table, UserBlacklistsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate:          function (val, date) { return date; },
            formatDateTime:      function (val, date) { return date; },
            formatDateTimeRaw:   function (val, date) { return date; },
            formatTime:          function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _Table_, _Notifications_, _UserBlacklistsResource_) {
        UserBlacklistsResource = _UserBlacklistsResource_;
        Notifications = _Notifications_;
        Table = _Table_;
        $httpBackend = _$httpBackend_;
        $scope = $rootScope.$new();
        $controller('UserblacklistsCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';

        $httpBackend.whenGET('/admin-ng/resources/userblacklists/filters.json')
            .respond(200);
        $httpBackend.whenGET('/blacklist/blacklists.json?limit=10&offset=0&type=person')
            .respond(JSON.stringify(getJSONFixture('blacklist/blacklists.json')));
        $httpBackend.whenGET('/blacklist/241?type=person')
            .respond(JSON.stringify(getJSONFixture('blacklist/241')));
    });

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('userblacklists');
    });

    describe('#delete', function () {
        var blacklist;

        beforeEach(function () {
            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');
            blacklist = UserBlacklistsResource.get({ id: 241 });
            $httpBackend.flush();
        });

        it('deletes the blacklist item', function () {
            spyOn(blacklist, '$delete');
            $scope.table.delete(blacklist);

            expect(blacklist.$delete).toHaveBeenCalled();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenDELETE('/blacklist/241').respond(201);
            });

            it('shows a notification and refreshes the table', function () {
                $scope.table.delete(blacklist);
                $httpBackend.flush();

                expect(Table.fetch).toHaveBeenCalled();
                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
            });
        });

        describe('on error', function () {
            beforeEach(function () {
                $httpBackend.whenDELETE('/blacklist/241').respond(500);
            });

            it('shows a notification', function () {
                $scope.table.delete(blacklist);
                $httpBackend.flush();

                expect(Table.fetch).not.toHaveBeenCalled();
                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String));
            });
        });
    });
});
