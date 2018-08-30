describe('Schedule Task Controller', function () {
    var $scope, $controller, $httpBackend, TableServiceMock, FormNavigatorServiceMock, NotificationsMock, TaskResourceMock, $timeout;

    beforeEach(module('adminNg'));

    // initiate mocks
    TableServiceMock = jasmine.createSpyObj('TableService', ['fetch', 'copySelected', 'deselectAll']);
    FormNavigatorServiceMock = jasmine.createSpyObj('FormNavigatorService', ['navigateTo']);
    TaskResourceMock = jasmine.createSpyObj('TaskResource', ['save']);
    NotificationsMock = jasmine.createSpyObj('Notifications', ['add']);
    TableServiceMock.copySelected.and.returnValue([{id: 'row1', selected: true}, {id: 'row2', selected: true}]);

    beforeEach(module(function ($provide) {
        $provide.value('FormNavigatorService', FormNavigatorServiceMock);
        $provide.value('Notifications', NotificationsMock);
        $provide.value('TaskResource', TaskResourceMock);
        $provide.value('Table', TableServiceMock);
    }));
    // provide fake language service
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));
    beforeEach(inject(function ($rootScope, _$controller_, _$timeout_, _$httpBackend_) {
        $controller = _$controller_;
        $scope = $rootScope.$new();
        $scope.close = jasmine.createSpy('close');
        $timeout = _$timeout_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        $controller('ScheduleTaskCtrl', {$scope: $scope});
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/event/new/processing?tags=archive').respond(JSON.stringify(getJSONFixture('admin-ng/event/new/processing')));
        $httpBackend.whenPOST('/admin-ng/event/workflowProperties').respond("{}");
    });

    describe('basic functionality', function () {

        it('instantiation', function () {
            expect(TableServiceMock.copySelected).toHaveBeenCalled();
        });

        it('is decorated with table functionality', function () {
            expect($scope.hasAnySelected).toBeDefined();
        });
    });

    describe('submit', function () {
        beforeEach(function () {
            $scope.processing.ud.workflow.id = 'my workflow';
            $scope.processing.ud.workflow.selection = {
                configuration: {
                    opt1: true
                }
            };
            $scope.submit();
        });

        it('saves the task', function () {
            expect(TaskResourceMock.save).toHaveBeenCalled();
        });

        it('closes and notifies on success', function () {
            TaskResourceMock.save.calls.mostRecent().args[1].call($scope);
            $timeout.flush();
            expect(NotificationsMock.add).toHaveBeenCalledWith('success', 'TASK_CREATED');
            expect($scope.close).toHaveBeenCalled();
        });

        it('closes and notifies on failure', function () {
            TaskResourceMock.save.calls.mostRecent().args[2].call($scope);
            $timeout.flush();
            expect(NotificationsMock.add).toHaveBeenCalledWith('error', 'TASK_NOT_CREATED', 'global', -1);
            expect($scope.close).toHaveBeenCalled();
        });

        it('deselects all rows on success', function () {
            TaskResourceMock.save.calls.mostRecent().args[1].call($scope);
            $timeout.flush();
            expect(TableServiceMock.deselectAll).toHaveBeenCalled();
        });

    });
});
