describe('Edit events controller', function () {
    var $scope, $controller, $httpBackend, TableServiceMock, FormNavigatorServiceMock, NotificationsMock, SeriesResourceMock, $timeout;

    beforeEach(module('adminNg'));

    // initiate mocks
    TableServiceMock = jasmine.createSpyObj('TableService', ['fetch', 'copySelected', 'deselectAll']);
    FormNavigatorServiceMock = jasmine.createSpyObj('FormNavigatorService', ['navigateTo']);
    NotificationsMock = jasmine.createSpyObj('Notifications', ['add']);
    WizardHandlerMock = jasmine.createSpyObj('WizardHandler', { wizard: { next: function() {} } });
    TableServiceMock.copySelected.and.returnValue([
        {id: 'first', selected: true, series_id: "4581", title: "haha", agent_id: "agent1"},
        {id: 'second', selected: true, series_id: "4581", title: "hoho", agent_id: "agent1"}
        ]);

    beforeEach(module(function ($provide) {
        $provide.value('FormNavigatorService', FormNavigatorServiceMock);
        $provide.value('Notifications', NotificationsMock);
        $provide.value('Table', TableServiceMock);
        $provide.value('WizardHandler', WizardHandlerMock);
    }));
    // provide fake language service
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            formatDateTime: function (val, format, time) { return time; },
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));
    beforeEach(inject(function ($rootScope, _$controller_, _$timeout_, _$httpBackend_, _JsHelper_) {
        $controller = _$controller_;
        $scope = $rootScope.$new();
        $scope.close = jasmine.createSpy('close');
        $timeout = _$timeout_;
        JsHelper = _JsHelper_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        $controller('EditEventsCtrl', {$scope: $scope});
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        // These are the requests that are necessary to construct the modal.
        $httpBackend.expectGET('/admin-ng/series/series.json').respond(JSON.stringify(getJSONFixture('admin-ng/series/series.json')));
        $httpBackend.expectGET('/admin-ng/capture-agents/agents.json?inputs=true').respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
        $httpBackend.expectPOST('/admin-ng/event/scheduling.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/scheduling.json')));
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
        $httpBackend.flush();
    });

    describe('basic functionality', function () {

        it('instantiation', function () {
            expect(TableServiceMock.copySelected).toHaveBeenCalled();
        });

        it('is decorated with table functionality', function () {
            expect($scope.hasAnySelected).toBeDefined();
        });

        it('processes series information', function() {
            expect($scope.seriesResults).not.toEqual({});
        })

        it('processes capture agent', function() {
            expect($scope.captureAgents).not.toEqual([]);
        })
    });

    describe('wizard edit step', function () {
        it('is correctly instantiated', function () {
            spyOn(JsHelper, 'getTimeZoneName').and.returnValue("UTC");
            $scope.clearFormAndContinue();
            expect($scope.conflictCheckingEnabled).toBe(true);
            expect($scope.metadataRows).not.toBe([]);
            // Title is ambiguous
            expect($scope.metadataRows[0].value).toBe("");
            // Series is non-ambiguous
            expect($scope.metadataRows[1].value).toBe("4581");
            expect($scope.scheduling).not.toBe({});
            // Agent is non-ambiguous
            expect($scope.scheduling.location.id).toBe("agent1");
            // Yadda yadda, test the rest of the data. :)
        });
    });

    describe('wizard summary step', function () {
        it('is correctly instantiated', function () {
            spyOn(JsHelper, 'getTimeZoneName').and.returnValue("UTC");
            $scope.clearFormAndContinue();
            $scope.generateEventSummariesAndContinue();
            expect($scope.eventSummaries.length).toBe(0);
        });
    });
});
