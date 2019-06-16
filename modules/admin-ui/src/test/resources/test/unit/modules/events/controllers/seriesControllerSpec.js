describe('Series controller', function () {
    var $scope, $httpBackend, SeriesResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _SeriesResource_) {
        $scope = $rootScope.$new();
        $controller('SeriesCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
        SeriesResource = _SeriesResource_;
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('series');
    });


    describe('#delete', function () {

        it('deletes the series', function () {
            spyOn(SeriesResource, 'delete');
            $scope.table.delete({'id': 12});
            expect(SeriesResource.delete).toHaveBeenCalledWith({id: 12}, jasmine.any(Function), jasmine.any(Function));
        });

        it('reloads series after deletion', function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.expectGET('/admin-ng/resources/series/filters.json').respond('[]');
            $httpBackend.expectDELETE('/admin-ng/series/12').respond('12');
            $httpBackend.expectGET('/admin-ng/series/series.json?limit=10&offset=0&sort=title:ASC').respond(JSON.stringify(getJSONFixture('admin-ng/series/series.json')));
            $httpBackend.whenGET('/admin-ng/series/series.json?limit=10&offset=0&sort=title:ASC').respond(JSON.stringify(getJSONFixture('admin-ng/series/series.json')));
            $httpBackend.whenGET('modules/events/partials/index.html').respond('');

            $scope.table.delete({'id': 12});

            $httpBackend.flush();
        });

    });
});
