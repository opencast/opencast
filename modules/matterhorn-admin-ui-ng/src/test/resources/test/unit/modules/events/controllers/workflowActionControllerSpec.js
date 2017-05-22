describe('WorkflowActionCtrl Test', function () {
    var $scope, $controller, $httpBackend, Table, Modal, Notifications, EventWorkflowActionResource;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate:     function (val, time) { return time; },
            formatDateTime: function (val, time) { return time; },
            formatTime:     function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _Table_, _Notifications_, _Modal_, _EventWorkflowActionResource_) {
        $scope = $rootScope.$new();
    	$controller = _$controller_;
        $httpBackend = _$httpBackend_;
        Table = _Table_;
        Notifications = _Notifications_;
        Modal = _Modal_;
        EventWorkflowActionResource = _EventWorkflowActionResource_;

        $controller('WorkflowActionCtrl', {$scope: $scope});
    }));

    describe('#workflowAction', function () {
        beforeEach(function () {
            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');
            Modal.$scope = { close: jasmine.createSpy() };
            Modal.$scope.resourceId = 1234;
        });

        describe('on success', function () {
            beforeEach(function () {
            	$httpBackend.expectPUT(/\/admin-ng\/event\/.+\/workflow\/action\/.+/g).respond(200, '{}');
            });

            it('resumes workflow, shows notification, refreshes the table, close modal', function () {
                $scope.workflowAction('RETRY');
                $httpBackend.flush();

                expect(Table.fetch).toHaveBeenCalled();
                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
                expect(Modal.$scope.close).toHaveBeenCalled();
            });

            it('aborts workflow, shows notification, close modal', function () {
                $scope.workflowAction('NONE');
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
                expect(Modal.$scope.close).toHaveBeenCalled();
            });
        });

        describe('on error', function () {
            beforeEach(function () {
            	$httpBackend.expectPUT(/\/admin-ng\/event\/.+\/workflow\/action\/.+/g).respond(500, '{}');
            });

        	it('shows notification, closes modal', function () {
                $scope.workflowAction('RETRY');
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String));
                expect(Modal.$scope.close).toHaveBeenCalled();
            });

            it('shows notification, closes modal', function () {
                $scope.workflowAction('NONE');
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String));
                expect(Modal.$scope.close).toHaveBeenCalled();
            });
        });
    });
});
