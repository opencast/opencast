describe('Events controller', function () {
    var $scope, $httpBackend, EventsResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _EventsResource_) {
        $scope = $rootScope.$new();
        $controller('EventsCtrl', {$scope: $scope});
        $httpBackend = _$httpBackend_;
        EventsResource = _EventsResource_;
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
    }));

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('events');
    });

    describe('#delete', function () {

        it('deletes the event', function () {
            spyOn(EventsResource, 'delete');
            $scope.table.delete({'id': 12});
            expect(EventsResource.delete).toHaveBeenCalledWith({id: 12}, jasmine.any(Function), jasmine.any(Function));
        });

        it('reloads events after deletion', function () {
            $httpBackend.expectGET('/admin-ng/resources/STATS.json').respond('[]');
            $httpBackend.expectGET('/admin-ng/resources/events/filters.json').respond('[]');
            $httpBackend.expectGET('/admin-ng/resources/PUBLICATION.CHANNELS.json').respond('{}');
            $httpBackend.expectDELETE('/admin-ng/event/12').respond('12');
            $httpBackend.whenGET('/admin-ng/event/9cc888e8-cdf6-4974-bf18-effecdadfa94/comments').respond('[]')
            $httpBackend.whenGET('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/comments').respond('[]')
            $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments').respond('[]')
            $httpBackend.whenGET('/admin-ng/event/c990ea15-e5ed-4fcf-bc17-cb070091c343/comments').respond('[]')
            $httpBackend.whenGET('/admin-ng/event/events.json?limit=10&offset=0&sort=title:ASC').respond(JSON.stringify(getJSONFixture('admin-ng/event/events.json')));

            $scope.table.delete({'id': 12});

            $httpBackend.flush();
        });

    });
});
