describe('Bulk Delete controller', function () {
    var $scope, bulkDeleteResource, _Notifications, successfulSubmit, table, modal, selection;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        bulkDeleteResource = {
            delete: function (empty, what, success, failure) {
                if (successfulSubmit) {
                    success();
                } else {
                    failure();
                }
            }

        };
        $provide.value('BulkDeleteResource', bulkDeleteResource);

        table = {
            resource: 'events',
            getSelected: function () {
                return selection;
            },
            toggleAllSelectionFlags: function () {}
        };
        $provide.value('Table', table);

        modal = {
            $scope: {
                close: function () {}
            }
        };
        $provide.value('Modal', modal);
        selection = [{id: 1}, {id: 2}];
    }));

    beforeEach(inject(function ($rootScope, $controller, Notifications) {
        $scope = $rootScope.$new();
        $controller('BulkDeleteCtrl', {$scope: $scope});
        _Notifications = Notifications;
    }));

    it('instantiates', function () {
        expect($scope.rows).toBeDefined();
        expect($scope.rows.length).toEqual(2);
        expect($scope.submit).toBeDefined();
        expect($scope.toggleSelectAll).toBeDefined();
    });

    it('toggles the table selection', function () {
        spyOn(table, 'toggleAllSelectionFlags').and.callThrough();
        $scope.toggleSelectAll();
        expect(table.toggleAllSelectionFlags).toHaveBeenCalled();
    });

    it('checks validity', function () {
        expect($scope.valid()).toBeTruthy();
        selection = [];
        expect($scope.valid()).toBeFalsy();
    });

    it('submits bulk delete requests', function () {
        successfulSubmit = true;
        spyOn(bulkDeleteResource, 'delete').and.callThrough();
        $scope.submit();
        var expectedArgument = {resource: 'event', endpoint: 'deleteEvents', eventIds: [1, 2]},
        actualArguments = bulkDeleteResource.delete.calls.mostRecent();
        expect(actualArguments.args[1]).toEqual(expectedArgument);
    });

    it('notifies the user upon success', function () {
        successfulSubmit = true;
        spyOn(_Notifications, 'add').and.returnValue(true);
        $scope.submit();
        expect(_Notifications.add).toHaveBeenCalledWith('success', 'EVENTS_DELETED');
    });

    it('notifies the user upon failure', function () {
        successfulSubmit = false;
        spyOn(_Notifications, 'add').and.returnValue(true);
        $scope.submit();
        expect(_Notifications.add).toHaveBeenCalledWith('error', 'EVENTS_NOT_DELETED');
    });

    it('closes the modal window after success', function () {
        spyOn(modal.$scope, 'close');
        successfulSubmit = true;
        $scope.submit();
        expect(modal.$scope.close).toHaveBeenCalled();
    });

    it('closes the modal window after failure', function () {
        spyOn(modal.$scope, 'close');
        successfulSubmit = false;
        $scope.submit();
        expect(modal.$scope.close).toHaveBeenCalled();
    });
});
