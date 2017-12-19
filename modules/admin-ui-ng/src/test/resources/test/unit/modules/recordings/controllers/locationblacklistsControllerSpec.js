describe('Location Blacklists controller', function () {
    var $scope, $httpBackend, Notifications, Table, LocationBlacklistsResource;

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

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _Table_, _Notifications_, _LocationBlacklistsResource_) {
        LocationBlacklistsResource = _LocationBlacklistsResource_;
        Notifications = _Notifications_;
        Table = _Table_;
        $httpBackend = _$httpBackend_;
        $scope = $rootScope.$new();
        $controller('LocationblacklistsCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';

        $httpBackend.whenGET('/admin-ng/resources/locationblacklists/filters.json')
            .respond(200);
        $httpBackend.whenGET('/blacklist/241?type=room')
            .respond(JSON.stringify(getJSONFixture('blacklist/241')));
    });

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('locationblacklists');
    });

    describe('#delete', function () {
        var blacklist;

        beforeEach(function () {
            $httpBackend.whenGET('/blacklist/blacklists.json?limit=10&offset=0&type=room')
                .respond(JSON.stringify(getJSONFixture('blacklist/blacklists.json')));
            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');
            blacklist = LocationBlacklistsResource.get({ id: 241 });
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
